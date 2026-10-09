package com.surveysystem.api;

import com.surveysystem.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.HashMap;
import java.sql.PreparedStatement;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.surveysystem.service.EmailService;
import com.surveysystem.service.PasswordResetService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;
    private final String adminEmail;
    private final String adminBootstrapToken;

    public AuthController(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            PasswordResetService passwordResetService,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.bootstrap-token:}") String adminBootstrapToken) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminBootstrapToken = adminBootstrapToken;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE email = ?", Integer.class, email) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        boolean isBootstrapAdmin = !adminEmail.isBlank() && adminEmail.equals(email);
        if (isBootstrapAdmin
                && (adminBootstrapToken.isBlank() || !adminBootstrapToken.equals(request.adminBootstrapToken()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin account setup is not authorized");
        }
        String role = isBootstrapAdmin ? "ADMIN" : "USER";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO app_users(name, email, password_hash, role) VALUES (?, ?, ?, ?)",
                    new String[] {"id"});
            statement.setString(1, request.name().trim());
            statement.setString(2, email);
            statement.setString(3, passwordEncoder.encode(request.password()));
            statement.setString(4, role);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return the created user ID");
        }
        long id = key.longValue();
        Map<String, Object> result = new HashMap<>(session(id, request.name().trim(), email, role));
        result.put("emailSent", emailService.sendWelcome(request.name().trim(), email));
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        }
        var users = jdbc.query(
                "SELECT id, name, email, password_hash, role FROM app_users WHERE email = ?",
                (rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role")),
                email);
        if (users.isEmpty() || !passwordEncoder.matches(request.password(), users.getFirst().passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        }
        UserRow user = users.getFirst();
        return session(user.id(), user.name(), user.email(), user.role());
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody EmailRequest request) {
        if (!emailService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Password reset email is not configured");
        }
        try {
            passwordResetService.requestCode(request.email().trim().toLowerCase());
        } catch (EmailService.EmailNotConfiguredException | EmailService.EmailDeliveryException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Could not send the reset email. Try again later");
        }
        return Map.of("message", "If an account exists for that email, a reset code has been sent.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        boolean reset = passwordResetService.resetPassword(
                request.email().trim().toLowerCase(),
                request.code(),
                passwordEncoder.encode(request.password()));
        if (!reset) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The reset code is invalid or expired");
        }
        return Map.of("message", "Password updated. You can now sign in.");
    }

    private Map<String, Object> session(long id, String name, String email, String role) {
        return Map.of(
                "token", jwtService.createToken(id, email, role),
                "user", Map.of("id", id, "name", name, "email", email, "role", role));
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 255, message = "Email is too long") String email,
            @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") String password,
            String adminBootstrapToken) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record EmailRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address")
                    @Size(max = 255, message = "Email is too long") String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address")
                    @Size(max = 255, message = "Email is too long") String email,
            @NotBlank(message = "Reset code is required")
                    @Pattern(regexp = "\\d{6}", message = "Reset code must be 6 digits") String code,
            @NotBlank(message = "Password is required")
                    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") String password) {
    }

    private record UserRow(long id, String name, String email, String passwordHash, String role) {
    }
}

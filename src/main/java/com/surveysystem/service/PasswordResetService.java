package com.surveysystem.service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Duration otpExpiration;
    private final Duration resendCooldown;
    private final int maxAttempts;

    public PasswordResetService(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.password-reset.otp-expiration:PT10M}") Duration otpExpiration,
            @Value("${app.password-reset.resend-cooldown:PT1M}") Duration resendCooldown,
            @Value("${app.password-reset.max-attempts:5}") int maxAttempts) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpExpiration = otpExpiration;
        this.resendCooldown = resendCooldown;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public void requestCode(String email) {
        List<Long> accounts = jdbc.query(
                "SELECT id FROM app_users WHERE email = ? FOR UPDATE",
                (rs, rowNum) -> rs.getLong(1), email);
        if (accounts.isEmpty()) {
            return;
        }

        List<Boolean> recentRequests = jdbc.query(
                "SELECT requested_at > ? FROM password_reset_otps WHERE email = ?",
                (rs, rowNum) -> rs.getBoolean(1),
                Timestamp.from(Instant.now().minus(resendCooldown)), email);
        if (!recentRequests.isEmpty() && recentRequests.getFirst()) {
            return;
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String hash = passwordEncoder.encode(code);
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(otpExpiration));
        int updated = jdbc.update(
                "UPDATE password_reset_otps SET otp_hash = ?, expires_at = ?, requested_at = ?, attempts = 0 "
                        + "WHERE email = ?",
                hash, expiresAt, now, email);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO password_reset_otps(email, otp_hash, expires_at, requested_at, attempts) "
                            + "VALUES (?, ?, ?, ?, 0)",
                    email, hash, expiresAt, now);
        }
        emailService.sendPasswordResetCode(email, code);
    }

    @Transactional
    public boolean resetPassword(String email, String code, String newPasswordHash) {
        List<ResetRequest> requests = jdbc.query(
                "SELECT otp_hash, expires_at, attempts FROM password_reset_otps WHERE email = ? FOR UPDATE",
                (rs, rowNum) -> new ResetRequest(
                        rs.getString("otp_hash"),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getInt("attempts")),
                email);
        if (requests.isEmpty()) {
            return false;
        }

        ResetRequest reset = requests.getFirst();
        if (reset.attempts() >= maxAttempts || !reset.expiresAt().isAfter(Instant.now())) {
            return false;
        }
        if (!passwordEncoder.matches(code, reset.otpHash())) {
            jdbc.update("UPDATE password_reset_otps SET attempts = attempts + 1 WHERE email = ?", email);
            return false;
        }

        int updated = jdbc.update("UPDATE app_users SET password_hash = ? WHERE email = ?", newPasswordHash, email);
        if (updated == 0) {
            return false;
        }
        jdbc.update("DELETE FROM password_reset_otps WHERE email = ?", email);
        return true;
    }

    private record ResetRequest(String otpHash, Instant expiresAt, int attempts) {
    }
}

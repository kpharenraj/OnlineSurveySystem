package com.surveysystem.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbc;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return jdbc.query(
                "SELECT id, name, email, role, created_at AS \"createdAt\" FROM app_users ORDER BY created_at DESC",
                (rs, rowNum) -> Map.of(
                        "id", rs.getLong("id"),
                        "name", rs.getString("name"),
                        "email", rs.getString("email"),
                        "role", rs.getString("role"),
                        "createdAt", rs.getTimestamp("createdAt").toInstant()));
    }

    @GetMapping("/surveys")
    public List<Map<String, Object>> surveys() {
        return jdbc.query(
                "SELECT s.id, s.title, s.status, s.public_token AS \"publicToken\", u.email AS \"ownerEmail\", "
                        + "(SELECT COUNT(*) FROM responses r WHERE r.survey_id = s.id) AS \"responseCount\" "
                        + "FROM surveys s JOIN app_users u ON u.id = s.user_id ORDER BY s.created_at DESC",
                (rs, rowNum) -> Map.of(
                        "id", rs.getLong("id"),
                        "title", rs.getString("title"),
                        "status", rs.getString("status"),
                        "ownerEmail", rs.getString("ownerEmail"),
                        "responseCount", rs.getLong("responseCount"),
                        "publicToken", rs.getString("publicToken") == null ? "" : rs.getString("publicToken")));
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(Authentication authentication, @PathVariable long userId) {
        Long currentUserId = jdbc.query(
                "SELECT id FROM app_users WHERE email = ?",
                (rs, rowNum) -> rs.getLong(1), authentication.getName()).stream().findFirst().orElse(null);
        if (currentUserId != null && currentUserId == userId) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrators cannot delete their own account");
        }
        if (jdbc.update("DELETE FROM app_users WHERE id = ?", userId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    @PostMapping("/surveys/{surveyId}/disable")
    public Map<String, String> disableSurvey(@PathVariable long surveyId) {
        if (jdbc.update("UPDATE surveys SET status = 'CLOSED', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                surveyId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Survey not found");
        }
        return Map.of("message", "Survey disabled");
    }
}

package com.surveysystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:surveytest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long"
        })
class SurveyApplicationIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void ownerCanPublishCollectAndAnalyzeResponses() {
        assertThat(get("/api/surveys", null, Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String email = "owner-" + UUID.randomUUID() + "@example.com";
        Map<?, ?> account = post("/api/auth/register",
                Map.of("name", "Survey Owner", "email", email, "password", "Password123!"), null, Map.class).getBody();
        assertThat(account.containsKey("token")).isTrue();
        String token = account.get("token").toString();

        Map<?, ?> survey = post("/api/surveys",
                Map.of("title", "Workshop feedback", "description", "Tell us what you think."),
                token, Map.class).getBody();
        long surveyId = ((Number) survey.get("id")).longValue();

        Map<?, ?> choiceQuestion = post("/api/surveys/" + surveyId + "/questions",
                Map.of(
                        "questionText", "What did you enjoy?",
                        "questionType", "SINGLE_CHOICE",
                        "required", true,
                        "options", List.of("The talk", "The activities")),
                token, Map.class).getBody();
        Map<?, ?> ratingQuestion = post("/api/surveys/" + surveyId + "/questions",
                Map.of(
                        "questionText", "How would you rate it?",
                        "questionType", "RATING",
                        "required", true,
                        "options", List.of()),
                token, Map.class).getBody();

        Map<?, ?> published = post("/api/surveys/" + surveyId + "/publish", Map.of(), token, Map.class).getBody();
        String publicToken = published.get("publicToken").toString();
        ResponseEntity<String> publicPage = get("/survey/" + publicToken, null, String.class);
        assertThat(publicPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicPage.getBody()).contains("<main id=\"app\"");
        assertThat(get("/api/public/surveys/" + publicToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> invalid = post("/api/public/surveys/" + publicToken + "/responses",
                Map.of("answers", List.of(
                        Map.of("questionId", choiceQuestion.get("id"), "answer", "The talk"),
                        Map.of("questionId", ratingQuestion.get("id"), "answer", 8))),
                null, Map.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> missingRequired = post("/api/public/surveys/" + publicToken + "/responses",
                Map.of("answers", List.of(
                        Map.of("questionId", choiceQuestion.get("id"), "answer", "The talk"))),
                null, Map.class);
        assertThat(missingRequired.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> submitted = post("/api/public/surveys/" + publicToken + "/responses",
                Map.of("answers", List.of(
                        Map.of("questionId", choiceQuestion.get("id"), "answer", "The talk"),
                        Map.of("questionId", ratingQuestion.get("id"), "answer", 5))),
                null, Map.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<?, ?> analytics = get("/api/surveys/" + surveyId + "/analytics", token, Map.class).getBody();
        assertThat(((Number) analytics.get("totalResponses")).longValue()).isEqualTo(1);
        assertThat(((Number) analytics.get("averageRating")).doubleValue()).isEqualTo(5);
        assertThat(get("/api/surveys", token, List.class).getBody()).hasSize(1);

        String otherToken = post("/api/auth/register",
                Map.of("name", "Other User", "email", "other-" + UUID.randomUUID() + "@example.com",
                        "password", "Password123!"), null, Map.class).getBody().get("token").toString();
        assertThat(get("/api/surveys/" + surveyId, otherToken, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void homepageAndStaticAssetsAreServed() {
        assertThat(get("/", null, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/app.js", null, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private <T> ResponseEntity<T> post(String path, Object body, String token, Class<T> responseType) {
        return rest.exchange(url(path), HttpMethod.POST, entity(body, token), responseType);
    }

    private <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
        return rest.exchange(url(path), HttpMethod.GET, entity(null, token), responseType);
    }

    private HttpEntity<Object> entity(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}

package com.surveysystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surveysystem.api.ApiException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveyService {
    private static final Set<String> QUESTION_TYPES = Set.of(
            "TEXT", "LONG_TEXT", "SINGLE_CHOICE", "MULTIPLE_CHOICE", "RATING", "YES_NO");
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SurveyService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> listSurveys(String email) {
        return jdbc.query(
                "SELECT s.id, s.title, s.description, s.status, s.public_token, s.created_at, s.updated_at, "
                        + "(SELECT COUNT(*) FROM responses r WHERE r.survey_id = s.id) AS response_count "
                        + "FROM surveys s JOIN app_users u ON u.id = s.user_id WHERE u.email = ? ORDER BY s.updated_at DESC",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("title", rs.getString("title"));
                    row.put("description", rs.getString("description"));
                    row.put("status", rs.getString("status"));
                    row.put("publicToken", rs.getString("public_token"));
                    row.put("createdAt", rs.getTimestamp("created_at").toInstant());
                    row.put("updatedAt", rs.getTimestamp("updated_at").toInstant());
                    row.put("responseCount", rs.getLong("response_count"));
                    return row;
                },
                email);
    }

    public Map<String, Object> getSurvey(String email, long surveyId) {
        Map<String, Object> survey = ownedSurvey(email, surveyId);
        survey.put("questions", getQuestions(surveyId));
        return survey;
    }

    @Transactional
    public Map<String, Object> createSurvey(String email, String title, String description) {
        long userId = userId(email);
        long id = insert("INSERT INTO surveys(user_id, title, description) VALUES (?, ?, ?)",
                userId, title.trim(), description == null ? "" : description.trim());
        return getSurvey(email, id);
    }

    @Transactional
    public Map<String, Object> updateSurvey(String email, long surveyId, String title, String description) {
        ownedSurvey(email, surveyId);
        jdbc.update("UPDATE surveys SET title = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                title.trim(), description == null ? "" : description.trim(), surveyId);
        return getSurvey(email, surveyId);
    }

    @Transactional
    public void deleteSurvey(String email, long surveyId) {
        ownedSurvey(email, surveyId);
        jdbc.update("DELETE FROM surveys WHERE id = ?", surveyId);
    }

    @Transactional
    public Map<String, Object> addQuestion(
            String email, long surveyId, String text, String type, Integer requestedOrder,
            boolean required, List<String> options) {
        Map<String, Object> survey = ownedSurvey(email, surveyId);
        if ("PUBLISHED".equals(survey.get("status"))) {
            throw new ApiException(HttpStatus.CONFLICT, "Close the survey before editing its questions");
        }
        int nextOrder = jdbc.queryForObject(
                "SELECT COALESCE(MAX(question_order), 0) + 1 FROM questions WHERE survey_id = ?",
                Integer.class, surveyId);
        int order = requestedOrder == null ? nextOrder : requestedOrder;
        if (order < 1 || order > nextOrder) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Question order must be between 1 and " + nextOrder);
        }
        jdbc.update("UPDATE questions SET question_order = question_order + 1 "
                + "WHERE survey_id = ? AND question_order >= ?", surveyId, order);
        long questionId = insertQuestion(surveyId, text, type, order, required, options);
        touchSurvey(surveyId);
        return question(questionId);
    }

    @Transactional
    public Map<String, Object> updateQuestion(
            String email, long questionId, String text, String type, boolean required, List<String> options) {
        long surveyId = questionSurveyId(questionId);
        Map<String, Object> survey = ownedSurvey(email, surveyId);
        if ("PUBLISHED".equals(survey.get("status"))) {
            throw new ApiException(HttpStatus.CONFLICT, "Close the survey before editing its questions");
        }
        validateQuestion(type, options);
        jdbc.update("UPDATE questions SET question_text = ?, question_type = ?, required = ? WHERE id = ?",
                text.trim(), type, required, questionId);
        replaceOptions(questionId, type, options);
        touchSurvey(surveyId);
        return question(questionId);
    }

    @Transactional
    public void deleteQuestion(String email, long questionId) {
        long surveyId = questionSurveyId(questionId);
        if ("PUBLISHED".equals(ownedSurvey(email, surveyId).get("status"))) {
            throw new ApiException(HttpStatus.CONFLICT, "Close the survey before editing its questions");
        }
        jdbc.update("DELETE FROM questions WHERE id = ?", questionId);
        touchSurvey(surveyId);
    }

    @Transactional
    public void reorderQuestions(String email, long surveyId, List<Long> questionIds) {
        if ("PUBLISHED".equals(ownedSurvey(email, surveyId).get("status"))) {
            throw new ApiException(HttpStatus.CONFLICT, "Close the survey before editing its questions");
        }
        List<Long> currentIds = jdbc.query(
                "SELECT id FROM questions WHERE survey_id = ? ORDER BY question_order",
                (rs, rowNum) -> rs.getLong(1), surveyId);
        if (questionIds == null || questionIds.size() != currentIds.size()
                || new HashSet<>(questionIds).size() != questionIds.size()
                || !new HashSet<>(questionIds).equals(new HashSet<>(currentIds))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide each question ID exactly once");
        }
        for (int index = 0; index < questionIds.size(); index++) {
            jdbc.update("UPDATE questions SET question_order = ? WHERE id = ? AND survey_id = ?",
                    index + 1, questionIds.get(index), surveyId);
        }
        touchSurvey(surveyId);
    }

    @Transactional
    public Map<String, Object> publish(String email, long surveyId) {
        Map<String, Object> survey = ownedSurvey(email, surveyId);
        int questionCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE survey_id = ?", Integer.class, surveyId);
        if (questionCount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add at least one question before publishing");
        }
        jdbc.update(
                "UPDATE surveys SET status = 'PUBLISHED', public_token = COALESCE(public_token, ?), "
                        + "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                UUID.randomUUID().toString().replace("-", ""), surveyId);
        return getSurvey(email, surveyId);
    }

    @Transactional
    public Map<String, Object> close(String email, long surveyId) {
        ownedSurvey(email, surveyId);
        jdbc.update("UPDATE surveys SET status = 'CLOSED', updated_at = CURRENT_TIMESTAMP WHERE id = ?", surveyId);
        return getSurvey(email, surveyId);
    }

    public Map<String, Object> getPublicSurvey(String token) {
        Long surveyId = publicSurveyId(token);
        return surveyById(surveyId);
    }

    @Transactional
    public Map<String, Object> submitResponse(String token, List<AnswerInput> answerInputs) {
        long surveyId = publicSurveyId(token);
        List<Map<String, Object>> questions = getQuestions(surveyId);
        Map<Long, Map<String, Object>> questionById = new LinkedHashMap<>();
        for (Map<String, Object> question : questions) {
            questionById.put((Long) question.get("id"), question);
        }
        Map<Long, String> answerByQuestion = new LinkedHashMap<>();
        Set<Long> seenQuestionIds = new HashSet<>();
        if (answerInputs == null) {
            answerInputs = List.of();
        }
        for (AnswerInput input : answerInputs) {
            if (input == null || input.questionId() == null || input.answer() == null
                    || !questionById.containsKey(input.questionId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "An answer references an invalid question");
            }
            if (!seenQuestionIds.add(input.questionId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A question can only be answered once");
            }
            Map<String, Object> question = questionById.get(input.questionId());
            String answer = answerString(input.answer());
            if ((answer.isBlank() || (input.answer().isArray() && input.answer().isEmpty()))
                    && !Boolean.TRUE.equals(question.get("required"))) {
                continue;
            }
            validateAnswer(question, input.answer(), answer);
            answerByQuestion.put(input.questionId(), answer);
        }
        for (Map<String, Object> question : questions) {
            Long id = (Long) question.get("id");
            if (Boolean.TRUE.equals(question.get("required"))
                    && (answerByQuestion.get(id) == null || answerByQuestion.get(id).isBlank())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Please answer the required question: " + question.get("questionText"));
            }
        }
        long responseId = insert("INSERT INTO responses(survey_id) VALUES (?)", surveyId);
        for (Map.Entry<Long, String> answer : answerByQuestion.entrySet()) {
            jdbc.update("INSERT INTO answers(response_id, question_id, answer_text) VALUES (?, ?, ?)",
                    responseId, answer.getKey(), answer.getValue());
        }
        return Map.of("message", "Response submitted successfully");
    }

    public Map<String, Object> analytics(String email, long surveyId) {
        ownedSurvey(email, surveyId);
        long responseCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM responses WHERE survey_id = ?", Long.class, surveyId);
        List<Map<String, Object>> questions = getQuestions(surveyId);
        List<Map<String, Object>> questionAnalytics = new ArrayList<>();
        List<Integer> ratings = new ArrayList<>();
        for (Map<String, Object> question : questions) {
            long questionId = (Long) question.get("id");
            String type = (String) question.get("questionType");
            List<String> answers = jdbc.query(
                    "SELECT a.answer_text FROM answers a JOIN responses r ON r.id = a.response_id "
                            + "WHERE r.survey_id = ? AND a.question_id = ?",
                    (rs, rowNum) -> rs.getString(1), surveyId, questionId);
            Map<String, Long> distribution = new LinkedHashMap<>();
            double questionRatingSum = 0;
            int questionRatingCount = 0;
            for (String answer : answers) {
                if ("RATING".equals(type)) {
                    try {
                        int value = Integer.parseInt(answer);
                        ratings.add(value);
                        distribution.merge(Integer.toString(value), 1L, Long::sum);
                        questionRatingSum += value;
                        questionRatingCount++;
                    } catch (NumberFormatException ignored) {
                        // Stored rating answers are validated before insertion.
                    }
                } else if ("MULTIPLE_CHOICE".equals(type)) {
                    for (String choice : parseStringArray(answer)) {
                        distribution.merge(choice, 1L, Long::sum);
                    }
                } else {
                    distribution.merge(answer, 1L, Long::sum);
                }
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("questionId", questionId);
            summary.put("questionText", question.get("questionText"));
            summary.put("questionType", type);
            summary.put("answerCount", answers.size());
            summary.put("distribution", distribution);
            if (questionRatingCount > 0) {
                summary.put("averageRating", questionRatingSum / questionRatingCount);
            }
            questionAnalytics.add(summary);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalResponses", responseCount);
        result.put("totalQuestions", questions.size());
        result.put("averageRating", ratings.isEmpty() ? null
                : ratings.stream().mapToInt(Integer::intValue).average().orElse(0));
        result.put("questions", questionAnalytics);
        return result;
    }

    public List<Map<String, Object>> responses(String email, long surveyId) {
        ownedSurvey(email, surveyId);
        List<Long> responseIds = jdbc.query(
                "SELECT id FROM responses WHERE survey_id = ? ORDER BY submitted_at DESC",
                (rs, rowNum) -> rs.getLong(1), surveyId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long responseId : responseIds) {
            Map<String, Object> response = jdbc.queryForMap(
                    "SELECT id, submitted_at AS \"submittedAt\" FROM responses WHERE id = ?", responseId);
            response.put("answers", jdbc.query(
                    "SELECT q.question_text AS \"questionText\", q.question_type AS \"questionType\", a.answer_text AS answer "
                            + "FROM answers a JOIN questions q ON q.id = a.question_id "
                            + "WHERE a.response_id = ? ORDER BY q.question_order",
                    (rs, rowNum) -> Map.of(
                            "questionText", rs.getString("questionText"),
                            "answer", decodeAnswer(rs.getString("answer"), rs.getString("questionType"))),
                    responseId));
            result.add(response);
        }
        return result;
    }

    private long insertQuestion(long surveyId, String text, String type, int order, boolean required,
            List<String> options) {
        validateQuestion(type, options);
        long id = insert(
                "INSERT INTO questions(survey_id, question_text, question_type, question_order, required) "
                        + "VALUES (?, ?, ?, ?, ?)",
                surveyId, text.trim(), type, order, required);
        replaceOptions(id, type, options);
        return id;
    }

    private void validateQuestion(String type, List<String> options) {
        if (type == null || !QUESTION_TYPES.contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a supported question type");
        }
        List<String> cleanOptions = options == null ? List.of() : options.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .toList();
        if ("SINGLE_CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type)) {
            if (cleanOptions.size() < 2 || cleanOptions.size() > 20) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Choice questions need between 2 and 20 options");
            }
            if (new HashSet<>(cleanOptions).size() != cleanOptions.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Question options must be unique");
            }
        }
    }

    private void replaceOptions(long questionId, String type, List<String> options) {
        jdbc.update("DELETE FROM question_options WHERE question_id = ?", questionId);
        List<String> cleanOptions = switch (type) {
            case "YES_NO" -> List.of("Yes", "No");
            case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> options.stream()
                    .map(String::trim).filter(value -> !value.isBlank()).toList();
            default -> List.of();
        };
        for (int index = 0; index < cleanOptions.size(); index++) {
            jdbc.update("INSERT INTO question_options(question_id, option_text, option_order) VALUES (?, ?, ?)",
                    questionId, cleanOptions.get(index), index + 1);
        }
    }

    private void validateAnswer(Map<String, Object> question, JsonNode rawAnswer, String answer) {
        String type = (String) question.get("questionType");
        if (answer.length() > 4000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Answers must be at most 4000 characters");
        }
        if (answer.isBlank()) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) question.get("options");
        switch (type) {
            case "TEXT", "LONG_TEXT" -> {
                if (!rawAnswer.isTextual()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Enter text for this question");
                }
                if ("TEXT".equals(type) && answer.length() > 500) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Short text answers must be at most 500 characters");
                }
            }
            case "SINGLE_CHOICE", "YES_NO" -> {
                if (!rawAnswer.isTextual() || !options.contains(answer)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid option");
                }
            }
            case "MULTIPLE_CHOICE" -> {
                if (!rawAnswer.isArray() || rawAnswer.isEmpty()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Choose at least one valid option");
                }
                Set<String> selected = new HashSet<>();
                rawAnswer.forEach(node -> {
                    if (!node.isTextual() || !options.contains(node.asText()) || !selected.add(node.asText())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Choose valid, non-duplicate options");
                    }
                });
            }
            case "RATING" -> {
                try {
                    int rating = Integer.parseInt(answer);
                    if ((!rawAnswer.isNumber() && !rawAnswer.isTextual()) || rating < 1 || rating > 5) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Rating must be a number from 1 to 5");
                    }
                } catch (NumberFormatException exception) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Rating must be a number from 1 to 5");
                }
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported question type");
        }
    }

    private String answerString(JsonNode answer) {
        if (answer.isNull()) {
            return "";
        }
        if (answer.isTextual()) {
            return answer.asText().trim();
        }
        try {
            return objectMapper.writeValueAsString(answer);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Answer could not be read");
        }
    }

    private List<String> parseStringArray(String value) {
        try {
            return objectMapper.readValue(value,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private Object decodeAnswer(String value, String questionType) {
        if (!"MULTIPLE_CHOICE".equals(questionType)) {
            return value;
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            return node.isArray() ? objectMapper.convertValue(node, List.class) : value;
        } catch (JsonProcessingException exception) {
            return value;
        }
    }

    private Map<String, Object> ownedSurvey(String email, long surveyId) {
        List<Map<String, Object>> surveys = jdbc.query(
                "SELECT s.id, s.title, s.description, s.status, s.public_token, s.created_at, s.updated_at, "
                        + "(SELECT COUNT(*) FROM responses r WHERE r.survey_id = s.id) AS response_count "
                        + "FROM surveys s JOIN app_users u ON u.id = s.user_id WHERE u.email = ? AND s.id = ?",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("title", rs.getString("title"));
                    row.put("description", rs.getString("description"));
                    row.put("status", rs.getString("status"));
                    row.put("publicToken", rs.getString("public_token"));
                    row.put("createdAt", rs.getTimestamp("created_at").toInstant());
                    row.put("updatedAt", rs.getTimestamp("updated_at").toInstant());
                    row.put("responseCount", rs.getLong("response_count"));
                    return row;
                },
                email, surveyId);
        if (surveys.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Survey not found");
        }
        return surveys.getFirst();
    }

    private Map<String, Object> surveyById(long surveyId) {
        Map<String, Object> survey = jdbc.query(
                "SELECT id, title, description, status, public_token AS \"publicToken\", created_at AS \"createdAt\" "
                        + "FROM surveys WHERE id = ? AND status = 'PUBLISHED'",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("title", rs.getString("title"));
                    row.put("description", rs.getString("description"));
                    row.put("status", rs.getString("status"));
                    row.put("publicToken", rs.getString("publicToken"));
                    row.put("questions", getQuestions(surveyId));
                    return row;
                }, surveyId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This survey is unavailable"));
        return survey;
    }

    private List<Map<String, Object>> getQuestions(long surveyId) {
        List<Map<String, Object>> questions = jdbc.query(
                "SELECT id, question_text, question_type, question_order, required FROM questions "
                        + "WHERE survey_id = ? ORDER BY question_order, id",
                (rs, rowNum) -> {
                    Map<String, Object> question = new LinkedHashMap<>();
                    question.put("id", rs.getLong("id"));
                    question.put("questionText", rs.getString("question_text"));
                    question.put("questionType", rs.getString("question_type"));
                    question.put("order", rs.getInt("question_order"));
                    question.put("required", rs.getBoolean("required"));
                    question.put("options", jdbc.query(
                            "SELECT option_text FROM question_options WHERE question_id = ? ORDER BY option_order, id",
                            (optionRs, optionRow) -> optionRs.getString(1), rs.getLong("id")));
                    return question;
                },
                surveyId);
        return questions;
    }

    private Map<String, Object> question(long questionId) {
        long surveyId = questionSurveyId(questionId);
        return getQuestions(surveyId).stream()
                .filter(question -> question.get("id").equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    private long questionSurveyId(long questionId) {
        List<Long> ids = jdbc.query(
                "SELECT survey_id FROM questions WHERE id = ?",
                (rs, rowNum) -> rs.getLong(1), questionId);
        if (ids.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Question not found");
        }
        return ids.getFirst();
    }

    private long publicSurveyId(String token) {
        List<Long> ids = jdbc.query(
                "SELECT id FROM surveys WHERE public_token = ? AND status = 'PUBLISHED'",
                (rs, rowNum) -> rs.getLong(1), token);
        if (ids.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "This survey is unavailable");
        }
        return ids.getFirst();
    }

    private long userId(String email) {
        List<Long> ids = jdbc.query("SELECT id FROM app_users WHERE email = ?",
                (rs, rowNum) -> rs.getLong(1), email);
        if (ids.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again");
        }
        return ids.getFirst();
    }

    private void touchSurvey(long surveyId) {
        jdbc.update("UPDATE surveys SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", surveyId);
    }

    private long insert(String sql, Object... arguments) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return the created record ID");
        }
        return key.longValue();
    }

    public record AnswerInput(Long questionId, JsonNode answer) {
    }
}

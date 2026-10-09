package com.surveysystem.api;

import com.surveysystem.service.SurveyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SurveyController {
    private final SurveyService surveys;

    public SurveyController(SurveyService surveys) {
        this.surveys = surveys;
    }

    @GetMapping("/surveys")
    public List<Map<String, Object>> surveys(Authentication authentication) {
        return surveys.listSurveys(authentication.getName());
    }

    @GetMapping("/surveys/{surveyId}")
    public Map<String, Object> getSurvey(Authentication authentication, @PathVariable long surveyId) {
        return surveys.getSurvey(authentication.getName(), surveyId);
    }

    @PostMapping("/surveys")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createSurvey(
            Authentication authentication, @Valid @RequestBody SurveyRequest request) {
        return surveys.createSurvey(authentication.getName(), request.title(), request.description());
    }

    @PutMapping("/surveys/{surveyId}")
    public Map<String, Object> updateSurvey(
            Authentication authentication,
            @PathVariable long surveyId,
            @Valid @RequestBody SurveyRequest request) {
        return surveys.updateSurvey(authentication.getName(), surveyId, request.title(), request.description());
    }

    @DeleteMapping("/surveys/{surveyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSurvey(Authentication authentication, @PathVariable long surveyId) {
        surveys.deleteSurvey(authentication.getName(), surveyId);
    }

    @PostMapping("/surveys/{surveyId}/publish")
    public Map<String, Object> publish(Authentication authentication, @PathVariable long surveyId) {
        return surveys.publish(authentication.getName(), surveyId);
    }

    @PostMapping("/surveys/{surveyId}/close")
    public Map<String, Object> close(Authentication authentication, @PathVariable long surveyId) {
        return surveys.close(authentication.getName(), surveyId);
    }

    @PostMapping("/surveys/{surveyId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addQuestion(
            Authentication authentication,
            @PathVariable long surveyId,
            @Valid @RequestBody QuestionRequest request) {
        return surveys.addQuestion(authentication.getName(), surveyId, request.questionText(),
                request.questionType(), request.order(), request.required(), request.options());
    }

    @PutMapping("/questions/{questionId}")
    public Map<String, Object> updateQuestion(
            Authentication authentication,
            @PathVariable long questionId,
            @Valid @RequestBody QuestionRequest request) {
        return surveys.updateQuestion(authentication.getName(), questionId, request.questionText(),
                request.questionType(), request.required(), request.options());
    }

    @DeleteMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(Authentication authentication, @PathVariable long questionId) {
        surveys.deleteQuestion(authentication.getName(), questionId);
    }

    @PutMapping("/surveys/{surveyId}/questions/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderQuestions(
            Authentication authentication,
            @PathVariable long surveyId,
            @Valid @RequestBody ReorderRequest request) {
        surveys.reorderQuestions(authentication.getName(), surveyId, request.questionIds());
    }

    @GetMapping("/surveys/{surveyId}/analytics")
    public Map<String, Object> analytics(Authentication authentication, @PathVariable long surveyId) {
        return surveys.analytics(authentication.getName(), surveyId);
    }

    @GetMapping("/surveys/{surveyId}/responses")
    public List<Map<String, Object>> responses(Authentication authentication, @PathVariable long surveyId) {
        return surveys.responses(authentication.getName(), surveyId);
    }

    public record SurveyRequest(
            @NotBlank(message = "Survey title is required")
            @Size(max = 160, message = "Survey title must be at most 160 characters") String title,
            @Size(max = 2000, message = "Survey description must be at most 2000 characters") String description) {
    }

    public record QuestionRequest(
            @NotBlank(message = "Question text is required")
            @Size(max = 1000, message = "Question must be at most 1000 characters") String questionText,
            @NotBlank(message = "Question type is required") String questionType,
            Integer order,
            boolean required,
            List<String> options) {
    }

    public record ReorderRequest(List<Long> questionIds) {
    }
}

package com.surveysystem.api;

import com.surveysystem.service.SurveyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/surveys")
public class PublicSurveyController {
    private final SurveyService surveys;

    public PublicSurveyController(SurveyService surveys) {
        this.surveys = surveys;
    }

    @GetMapping("/{token}")
    public Map<String, Object> survey(@PathVariable String token) {
        return surveys.getPublicSurvey(token);
    }

    @PostMapping("/{token}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(
            @PathVariable String token, @Valid @RequestBody SubmitResponse request) {
        return surveys.submitResponse(token, request.answers());
    }

    public record SubmitResponse(List<SurveyService.AnswerInput> answers) {
    }
}

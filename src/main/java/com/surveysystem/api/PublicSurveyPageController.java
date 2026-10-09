package com.surveysystem.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicSurveyPageController {
    @GetMapping("/survey/{token}")
    public String surveyPage(@PathVariable String token) {
        return "forward:/index.html";
    }
}

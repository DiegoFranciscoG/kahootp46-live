package com.kahootp46.live.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.kahootp46.live.quiz.QuestionType;

public record QuestionRequest(
        @NotBlank @Size(max = 300) String text,
        QuestionType type,
        @NotNull Integer timeLimit,
        @NotNull @Size(min = 2, max = 6) List<@Valid @NotNull ChoiceRequest> choices) {

    public QuestionType typeOrDefault() {
        return type == null ? QuestionType.SINGLE_CHOICE : type;
    }
}

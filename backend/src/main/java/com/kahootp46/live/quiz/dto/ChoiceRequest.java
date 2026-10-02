package com.kahootp46.live.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChoiceRequest(
        @NotBlank @Size(max = 120) String text,
        boolean correct) {
}

package com.kahootp46.live.quiz.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kahootp46.live.quiz.QuestionType;

public record QuizResponse(
        UUID id,
        String title,
        String description,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionResponse> questions) {

    public record QuestionResponse(UUID id, int number, String text, QuestionType type, int timeLimit,
            List<ChoiceResponse> choices) {
    }

    public record ChoiceResponse(UUID id, int index, String text, boolean correct) {
    }
}

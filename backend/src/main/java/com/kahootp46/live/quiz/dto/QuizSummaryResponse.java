package com.kahootp46.live.quiz.dto;

import java.time.Instant;
import java.util.UUID;

public record QuizSummaryResponse(UUID id, String title, String description, int questionCount, Instant updatedAt) {
}

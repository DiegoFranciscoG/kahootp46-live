package com.kahootp46.live.game.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateGameRequest(@NotNull UUID quizId) {
}

package com.kahootp46.live.game.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.GameViews.ChoiceView;

/**
 * Host view of a game. Unlike the viewer snapshot, the host always sees the correct answer of the
 * current question because the quiz is theirs.
 */
public record HostGameResponse(
        UUID id,
        String pin,
        GameStatus status,
        UUID quizId,
        String quizTitle,
        int totalQuestions,
        Integer currentQuestion,
        HostQuestion question,
        long lastSeq,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {

    public record HostQuestion(int number, String text, List<ChoiceView> choices, int correctIndex,
            int timeLimitSeconds, Instant startedAt, Instant endsAt, boolean revealed) {
    }
}

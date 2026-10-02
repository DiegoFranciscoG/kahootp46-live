package com.kahootp46.live.game;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read models shared by the REST API, the WebSocket protocol and the snapshot cache.
 */
public final class GameViews {

    private GameViews() {
    }

    public record ChoiceView(int index, String text) {
    }

    /** A question as viewers see it: never contains the correct answer. */
    public record LiveQuestion(int questionNumber, int totalQuestions, String question, List<ChoiceView> choices,
            int timeLimitSeconds, Instant startedAt, Instant endsAt) {
    }

    /** Published only after the server reveals it (ANSWER_REVEALED). */
    public record RevealedAnswer(int questionNumber, int correctIndex, String correctText) {
    }

    public record HistoryItem(int questionNumber, String question, List<ChoiceView> choices, int correctIndex,
            String correctText) {
    }

    /**
     * Everything a viewer needs to (re)build its screen. {@code lastSeq} lets the client discard events it
     * already applied; {@code serverTime} lets it correct clock skew for the countdown.
     */
    public record GameSnapshot(UUID gameId, String pin, GameStatus status, String quizTitle, int totalQuestions,
            LiveQuestion currentQuestion, RevealedAnswer currentAnswer, List<HistoryItem> history, long lastSeq,
            Instant serverTime) {

        public GameSnapshot at(Instant now) {
            return new GameSnapshot(gameId, pin, status, quizTitle, totalQuestions, currentQuestion, currentAnswer,
                    history, lastSeq, now);
        }
    }
}

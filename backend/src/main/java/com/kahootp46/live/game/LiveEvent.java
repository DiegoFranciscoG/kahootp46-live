package com.kahootp46.live.game;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kahootp46.live.game.GameViews.ChoiceView;
import com.kahootp46.live.game.GameViews.GameSnapshot;

/**
 * KahootP46 Live protocol v1 (server -> client only). Documented in docs/protocolo-websocket.md.
 * Every event carries {@code seq}, a per-game counter that increases by one with each broadcast event.
 */
public sealed interface LiveEvent {

    int PROTOCOL_VERSION = 1;

    @JsonProperty("type")
    LiveEventType type();

    @JsonProperty("protocolVersion")
    default int protocolVersion() {
        return PROTOCOL_VERSION;
    }

    UUID gameId();

    String pin();

    long seq();

    Instant sentAt();

    enum RevealReason { TIME_UP, HOST }

    enum FinishReason { COMPLETED, CLOSED_BY_HOST }

    /** Sent only to the subscribing viewer, with the full state (initial connection and every reconnection). */
    @JsonPropertyOrder({"type", "protocolVersion", "gameId", "pin", "seq", "sentAt"})
    record GameConnected(UUID gameId, String pin, long seq, Instant sentAt, GameSnapshot snapshot) implements LiveEvent {
        @Override
        public LiveEventType type() {
            return LiveEventType.GAME_CONNECTED;
        }
    }

    @JsonPropertyOrder({"type", "protocolVersion", "gameId", "pin", "seq", "sentAt"})
    record GameStarted(UUID gameId, String pin, long seq, Instant sentAt, String quizTitle, int totalQuestions)
            implements LiveEvent {
        @Override
        public LiveEventType type() {
            return LiveEventType.GAME_STARTED;
        }
    }

    @JsonPropertyOrder({"type", "protocolVersion", "gameId", "pin", "seq", "sentAt"})
    record QuestionStarted(UUID gameId, String pin, long seq, Instant sentAt, int questionNumber, int totalQuestions,
            String question, List<ChoiceView> choices, int timeLimitSeconds, Instant startedAt, Instant endsAt)
            implements LiveEvent {
        @Override
        public LiveEventType type() {
            return LiveEventType.QUESTION_STARTED;
        }
    }

    @JsonPropertyOrder({"type", "protocolVersion", "gameId", "pin", "seq", "sentAt"})
    record AnswerRevealed(UUID gameId, String pin, long seq, Instant sentAt, int questionNumber, int correctIndex,
            String correctText, RevealReason reason) implements LiveEvent {
        @Override
        public LiveEventType type() {
            return LiveEventType.ANSWER_REVEALED;
        }
    }

    @JsonPropertyOrder({"type", "protocolVersion", "gameId", "pin", "seq", "sentAt"})
    record QuestionFinished(UUID gameId, String pin, long seq, Instant sentAt, int questionNumber)
            implements LiveEvent {
        @Override
        public LiveEventType type() {
            return LiveEventType.QUESTION_FINISHED;
        }
    }

    @JsonPropertyOrder({"type", "protocolVersion", "gameId", "pin", "seq", "sentAt"})
    record GameFinished(UUID gameId, String pin, long seq, Instant sentAt, int questionsPlayed, int totalQuestions,
            FinishReason reason) implements LiveEvent {
        @Override
        public LiveEventType type() {
            return LiveEventType.GAME_FINISHED;
        }
    }

    /** Sent only to the affected viewer; {@code gameId} is null when the PIN does not match any game. */
    @JsonPropertyOrder({"type", "protocolVersion", "gameId", "pin", "seq", "sentAt"})
    record GameError(UUID gameId, String pin, long seq, Instant sentAt, String code, String message)
            implements LiveEvent {
        @Override
        public LiveEventType type() {
            return LiveEventType.GAME_ERROR;
        }
    }
}

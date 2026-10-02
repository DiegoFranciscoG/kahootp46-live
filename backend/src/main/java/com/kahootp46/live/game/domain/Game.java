package com.kahootp46.live.game.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.shared.error.BusinessRuleException;

/**
 * A live session of a quiz. All state transitions live here so they can be unit tested without Spring.
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(nullable = false, length = 6)
    private String pin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(name = "current_question")
    private Integer currentQuestion;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "question_started_at")
    private Instant questionStartedAt;

    @Column(name = "question_ends_at")
    private Instant questionEndsAt;

    @Column(name = "last_seq", nullable = false)
    private long lastSeq;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Version
    private Long version;

    protected Game() {
    }

    public static Game create(UUID quizId, UUID hostId, String pin, int totalQuestions, Instant now) {
        if (totalQuestions < 1) {
            throw new BusinessRuleException("El quiz no tiene preguntas");
        }
        Game game = new Game();
        game.id = UUID.randomUUID();
        game.quizId = quizId;
        game.hostId = hostId;
        game.pin = pin;
        game.status = GameStatus.LOBBY;
        game.totalQuestions = totalQuestions;
        game.createdAt = now;
        return game;
    }

    public void start(int timeLimitSeconds, Instant now) {
        requireStatus(GameStatus.LOBBY, "La partida ya fue iniciada");
        startedAt = now;
        openQuestion(1, timeLimitSeconds, now);
    }

    public void reveal() {
        requireStatus(GameStatus.QUESTION_OPEN, "No hay una pregunta abierta para revelar");
        status = GameStatus.ANSWER_REVEALED;
    }

    public void advance(int timeLimitSeconds, Instant now) {
        requireStatus(GameStatus.ANSWER_REVEALED, "Primero se debe revelar la respuesta actual");
        if (!hasNextQuestion()) {
            throw new BusinessRuleException("No quedan más preguntas");
        }
        openQuestion(currentQuestion + 1, timeLimitSeconds, now);
    }

    public void finish(Instant now) {
        if (status == GameStatus.FINISHED) {
            throw new BusinessRuleException("La partida ya terminó");
        }
        if (status == GameStatus.QUESTION_OPEN) {
            throw new BusinessRuleException("Revela la respuesta antes de cerrar la partida");
        }
        status = GameStatus.FINISHED;
        finishedAt = now;
    }

    /** Reserves the next event sequence number; persisted with the game in the same transaction. */
    public long nextSeq() {
        return ++lastSeq;
    }

    public boolean hasNextQuestion() {
        return currentQuestion != null && currentQuestion < totalQuestions;
    }

    public boolean isHostedBy(UUID userId) {
        return hostId.equals(userId);
    }

    /** Number of questions whose answer has already been revealed. */
    public int revealedCount() {
        return switch (status) {
            case LOBBY -> 0;
            case QUESTION_OPEN -> currentQuestion - 1;
            case ANSWER_REVEALED, FINISHED -> currentQuestion == null ? 0 : currentQuestion;
        };
    }

    private void openQuestion(int number, int timeLimitSeconds, Instant now) {
        status = GameStatus.QUESTION_OPEN;
        currentQuestion = number;
        questionStartedAt = now;
        questionEndsAt = now.plusSeconds(timeLimitSeconds);
    }

    private void requireStatus(GameStatus expected, String message) {
        if (status != expected) {
            throw new BusinessRuleException(message);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuizId() {
        return quizId;
    }

    public UUID getHostId() {
        return hostId;
    }

    public String getPin() {
        return pin;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Integer getCurrentQuestion() {
        return currentQuestion;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public Instant getQuestionStartedAt() {
        return questionStartedAt;
    }

    public Instant getQuestionEndsAt() {
        return questionEndsAt;
    }

    public long getLastSeq() {
        return lastSeq;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}

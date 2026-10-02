package com.kahootp46.live.game.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.LiveEvent.FinishReason;
import com.kahootp46.live.game.LiveEvent.RevealReason;
import com.kahootp46.live.game.domain.Game;
import com.kahootp46.live.game.dto.HostGameResponse;
import com.kahootp46.live.game.dto.HostGameResponse.HostQuestion;
import com.kahootp46.live.game.repository.GameRepository;
import com.kahootp46.live.quiz.PlayableQuestion;
import com.kahootp46.live.quiz.PlayableQuiz;
import com.kahootp46.live.quiz.QuizCatalog;
import com.kahootp46.live.shared.error.BusinessRuleException;
import com.kahootp46.live.shared.error.ResourceNotFoundException;

/**
 * Game engine: host commands and question timeouts. The server is the only source of the correct answer
 * and decides when it is revealed.
 */
@Service
@Transactional
public class GameService {

    private static final int RECENT_GAMES = 20;

    private final GameRepository games;
    private final QuizCatalog quizzes;
    private final PinGenerator pins;
    private final LiveEventRecorder recorder;
    private final Clock clock;

    GameService(GameRepository games, QuizCatalog quizzes, PinGenerator pins, LiveEventRecorder recorder, Clock clock) {
        this.games = games;
        this.quizzes = quizzes;
        this.pins = pins;
        this.recorder = recorder;
        this.clock = clock;
    }

    public HostGameResponse create(UUID hostId, UUID quizId) {
        PlayableQuiz quiz = quizzes.findPlayable(quizId)
                .filter(found -> found.ownerId().equals(hostId))
                .orElseThrow(() -> new ResourceNotFoundException("Quiz no encontrado"));
        Game game = Game.create(quiz.id(), hostId, pins.generate(), quiz.totalQuestions(), clock.instant());
        games.saveAndFlush(game);
        return hostView(game, quiz);
    }

    public HostGameResponse start(UUID hostId, UUID gameId) {
        Game game = lockOwned(hostId, gameId);
        PlayableQuiz quiz = quizOf(game);
        Instant now = clock.instant();
        game.start(quiz.question(1).timeLimitSeconds(), now);
        recorder.record(game, seq -> GameProjections.gameStarted(game, quiz, seq, now));
        recorder.record(game, seq -> GameProjections.questionStarted(game, quiz, seq, now));
        return hostView(game, quiz);
    }

    public HostGameResponse reveal(UUID hostId, UUID gameId) {
        Game game = lockOwned(hostId, gameId);
        PlayableQuiz quiz = quizOf(game);
        revealCurrent(game, quiz, RevealReason.HOST, clock.instant());
        return hostView(game, quiz);
    }

    /** Closes the current question (revealing it first if needed) and opens the next one or ends the game. */
    public HostGameResponse next(UUID hostId, UUID gameId) {
        Game game = lockOwned(hostId, gameId);
        PlayableQuiz quiz = quizOf(game);
        Instant now = clock.instant();
        if (game.getStatus() == GameStatus.QUESTION_OPEN) {
            revealCurrent(game, quiz, RevealReason.HOST, now);
        }
        if (game.getStatus() != GameStatus.ANSWER_REVEALED) {
            throw new BusinessRuleException(game.getStatus() == GameStatus.LOBBY
                    ? "Inicia la partida para mostrar la primera pregunta"
                    : "La partida ya terminó");
        }
        recorder.record(game, seq -> GameProjections.questionFinished(game, seq, now));
        if (game.hasNextQuestion()) {
            PlayableQuestion nextQuestion = quiz.question(game.getCurrentQuestion() + 1);
            game.advance(nextQuestion.timeLimitSeconds(), now);
            recorder.record(game, seq -> GameProjections.questionStarted(game, quiz, seq, now));
        } else {
            game.finish(now);
            recorder.record(game, seq -> GameProjections.gameFinished(game, FinishReason.COMPLETED, seq, now));
        }
        return hostView(game, quiz);
    }

    /** "Cerrar partida": reveals and closes the current question, then ends the game. */
    public HostGameResponse finish(UUID hostId, UUID gameId) {
        Game game = lockOwned(hostId, gameId);
        PlayableQuiz quiz = quizOf(game);
        Instant now = clock.instant();
        if (game.getStatus() == GameStatus.QUESTION_OPEN) {
            revealCurrent(game, quiz, RevealReason.HOST, now);
        }
        if (game.getStatus() == GameStatus.ANSWER_REVEALED) {
            recorder.record(game, seq -> GameProjections.questionFinished(game, seq, now));
        }
        FinishReason reason = !game.hasNextQuestion() && game.getStatus() == GameStatus.ANSWER_REVEALED
                ? FinishReason.COMPLETED
                : FinishReason.CLOSED_BY_HOST;
        game.finish(now);
        recorder.record(game, seq -> GameProjections.gameFinished(game, reason, seq, now));
        return hostView(game, quiz);
    }

    /**
     * Called by the deadline scheduler. Idempotent: does nothing if the host already revealed or moved on.
     *
     * @return true if this call revealed the answer
     */
    public boolean revealOnTimeout(UUID gameId, int questionNumber) {
        Game game = games.findForUpdate(gameId).orElse(null);
        if (game == null || game.getStatus() != GameStatus.QUESTION_OPEN
                || !Objects.equals(game.getCurrentQuestion(), questionNumber)) {
            return false;
        }
        revealCurrent(game, quizOf(game), RevealReason.TIME_UP, clock.instant());
        return true;
    }

    @Transactional(readOnly = true)
    public HostGameResponse get(UUID hostId, UUID gameId) {
        Game game = games.findById(gameId)
                .filter(found -> found.isHostedBy(hostId))
                .orElseThrow(GameService::notFound);
        return hostView(game, quizOf(game));
    }

    @Transactional(readOnly = true)
    public List<HostGameResponse> recent(UUID hostId) {
        return games.findByHostIdOrderByCreatedAtDesc(hostId, Limit.of(RECENT_GAMES)).stream()
                .map(game -> hostView(game, quizOf(game)))
                .toList();
    }

    private void revealCurrent(Game game, PlayableQuiz quiz, RevealReason reason, Instant now) {
        game.reveal();
        recorder.record(game, seq -> GameProjections.answerRevealed(game, quiz, reason, seq, now));
    }

    private Game lockOwned(UUID hostId, UUID gameId) {
        return games.findForUpdate(gameId)
                .filter(game -> game.isHostedBy(hostId))
                .orElseThrow(GameService::notFound);
    }

    private PlayableQuiz quizOf(Game game) {
        return quizzes.findPlayable(game.getQuizId()).orElseThrow(GameService::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Partida no encontrada");
    }

    private static HostGameResponse hostView(Game game, PlayableQuiz quiz) {
        HostQuestion question = null;
        if (game.getCurrentQuestion() != null) {
            PlayableQuestion current = quiz.question(game.getCurrentQuestion());
            boolean revealed = game.getStatus() != GameStatus.QUESTION_OPEN;
            question = new HostQuestion(current.number(), current.text(), GameProjections.choices(current),
                    current.correctChoice().index(), current.timeLimitSeconds(), game.getQuestionStartedAt(),
                    game.getQuestionEndsAt(), revealed);
        }
        return new HostGameResponse(game.getId(), game.getPin(), game.getStatus(), game.getQuizId(), quiz.title(),
                game.getTotalQuestions(), game.getCurrentQuestion(), question, game.getLastSeq(), game.getCreatedAt(),
                game.getStartedAt(), game.getFinishedAt());
    }
}

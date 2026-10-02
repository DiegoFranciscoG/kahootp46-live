package com.kahootp46.live.game.scheduler;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.LiveEvent;
import com.kahootp46.live.game.repository.GameRepository;
import com.kahootp46.live.game.scheduler.QuestionDeadlineStore.Deadline;
import com.kahootp46.live.game.service.GameService;

/**
 * Reveals the answer automatically when a question's time runs out.
 */
@Component
class QuestionDeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuestionDeadlineScheduler.class);
    private static final int BATCH = 50;

    private final QuestionDeadlineStore store;
    private final GameService gameService;
    private final GameRepository games;
    private final Clock clock;

    QuestionDeadlineScheduler(QuestionDeadlineStore store, GameService gameService, GameRepository games, Clock clock) {
        this.store = store;
        this.gameService = gameService;
        this.games = games;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onQuestionStarted(LiveEvent.QuestionStarted event) {
        store.schedule(event.gameId(), event.questionNumber(), event.endsAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onAnswerRevealed(LiveEvent.AnswerRevealed event) {
        store.cancel(event.gameId(), event.questionNumber());
    }

    @Scheduled(fixedDelayString = "${app.game.deadline-poll-interval}")
    void fireDueDeadlines() {
        for (Deadline deadline : store.claimDue(clock.instant(), BATCH)) {
            try {
                gameService.revealOnTimeout(deadline.gameId(), deadline.questionNumber());
            } catch (RuntimeException ex) {
                log.warn("Could not reveal question {} of game {}", deadline.questionNumber(), deadline.gameId(), ex);
            }
        }
    }

    /** Redis may have restarted (free tiers do not persist): re-register deadlines of open questions. */
    @EventListener(ApplicationReadyEvent.class)
    void recoverOpenQuestions() {
        games.findByStatus(GameStatus.QUESTION_OPEN).forEach(game ->
                store.schedule(game.getId(), game.getCurrentQuestion(), game.getQuestionEndsAt()));
    }
}

package com.kahootp46.live.game.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.shared.error.BusinessRuleException;

class GameTest {

    private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

    private Game newGame(int questions) {
        return Game.create(UUID.randomUUID(), UUID.randomUUID(), "582941", questions, T0);
    }

    @Test
    void newGameWaitsInLobby() {
        Game game = newGame(3);

        assertThat(game.getStatus()).isEqualTo(GameStatus.LOBBY);
        assertThat(game.getCurrentQuestion()).isNull();
        assertThat(game.revealedCount()).isZero();
        assertThat(game.getLastSeq()).isZero();
    }

    @Test
    void startOpensQuestionOneWithItsDeadline() {
        Game game = newGame(3);

        game.start(20, T0);

        assertThat(game.getStatus()).isEqualTo(GameStatus.QUESTION_OPEN);
        assertThat(game.getCurrentQuestion()).isEqualTo(1);
        assertThat(game.getQuestionEndsAt()).isEqualTo(T0.plusSeconds(20));
        assertThat(game.getStartedAt()).isEqualTo(T0);
        assertThat(game.revealedCount()).isZero();
    }

    @Test
    void revealThenAdvanceMovesToNextQuestion() {
        Game game = newGame(2);
        game.start(20, T0);

        game.reveal();
        assertThat(game.getStatus()).isEqualTo(GameStatus.ANSWER_REVEALED);
        assertThat(game.revealedCount()).isEqualTo(1);

        game.advance(10, T0.plusSeconds(30));
        assertThat(game.getCurrentQuestion()).isEqualTo(2);
        assertThat(game.getStatus()).isEqualTo(GameStatus.QUESTION_OPEN);
        assertThat(game.getQuestionEndsAt()).isEqualTo(T0.plusSeconds(40));
        assertThat(game.hasNextQuestion()).isFalse();
    }

    @Test
    void cannotAdvanceWithoutRevealingOrPastTheLastQuestion() {
        Game game = newGame(1);
        game.start(20, T0);

        assertThatThrownBy(() -> game.advance(20, T0)).isInstanceOf(BusinessRuleException.class);
        game.reveal();
        assertThatThrownBy(() -> game.advance(20, T0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void invalidTransitionsAreRejected() {
        Game game = newGame(2);

        assertThatThrownBy(game::reveal).isInstanceOf(BusinessRuleException.class);
        game.start(20, T0);
        assertThatThrownBy(() -> game.start(20, T0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> game.finish(T0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void finishFromLobbyOrRevealEndsTheGame() {
        Game lobby = newGame(2);
        lobby.finish(T0);
        assertThat(lobby.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(lobby.revealedCount()).isZero();
        assertThatThrownBy(() -> lobby.finish(T0)).isInstanceOf(BusinessRuleException.class);

        Game played = newGame(2);
        played.start(20, T0);
        played.reveal();
        played.finish(T0.plusSeconds(5));
        assertThat(played.getFinishedAt()).isEqualTo(T0.plusSeconds(5));
        assertThat(played.revealedCount()).isEqualTo(1);
    }

    @Test
    void sequenceNumbersIncreaseByOne() {
        Game game = newGame(1);

        assertThat(game.nextSeq()).isEqualTo(1);
        assertThat(game.nextSeq()).isEqualTo(2);
        assertThat(game.getLastSeq()).isEqualTo(2);
    }

    @Test
    void quizWithoutQuestionsCannotBePlayed() {
        assertThatThrownBy(() -> newGame(0)).isInstanceOf(BusinessRuleException.class);
    }
}

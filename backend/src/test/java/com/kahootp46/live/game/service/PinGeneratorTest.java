package com.kahootp46.live.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.repository.GameRepository;
import com.kahootp46.live.shared.error.BusinessRuleException;

class PinGeneratorTest {

    private final GameRepository games = mock(GameRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void realGeneratorProducesSixDigitPins() {
        PinGenerator generator = new PinGenerator(games, clock);

        for (int i = 0; i < 200; i++) {
            assertThat(generator.generate()).matches("[1-9]\\d{5}");
        }
    }

    @Test
    void skipsPinsOfActiveOrRecentGames() {
        Iterator<Integer> candidates = List.of(111111, 222222, 333333).iterator();
        PinGenerator generator = new PinGenerator(games, clock, candidates::next);
        when(games.existsByPinAndStatusNot("111111", GameStatus.FINISHED)).thenReturn(true);
        when(games.existsByPinAndCreatedAtAfter(eq("222222"), any())).thenReturn(true);

        assertThat(generator.generate()).isEqualTo("333333");
    }

    @Test
    void givesUpAfterTooManyCollisions() {
        PinGenerator generator = new PinGenerator(games, clock, () -> 123456);
        when(games.existsByPinAndStatusNot(anyString(), any())).thenReturn(true);

        assertThatThrownBy(generator::generate).isInstanceOf(BusinessRuleException.class);
    }
}

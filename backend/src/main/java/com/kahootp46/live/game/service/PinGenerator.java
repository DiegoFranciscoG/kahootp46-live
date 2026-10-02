package com.kahootp46.live.game.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.function.IntSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.repository.GameRepository;
import com.kahootp46.live.shared.error.BusinessRuleException;

/**
 * 6-digit PIN from a CSPRNG. A PIN is never shared by two unfinished games and is not reused for 24 h,
 * so a viewer holding an old PIN cannot land in someone else's new game.
 */
@Component
class PinGenerator {

    static final Duration REUSE_COOLDOWN = Duration.ofHours(24);
    private static final int MAX_ATTEMPTS = 20;

    private final GameRepository games;
    private final Clock clock;
    private final IntSupplier randomSixDigits;

    @Autowired
    PinGenerator(GameRepository games, Clock clock) {
        this(games, clock, secureSixDigits());
    }

    PinGenerator(GameRepository games, Clock clock, IntSupplier randomSixDigits) {
        this.games = games;
        this.clock = clock;
        this.randomSixDigits = randomSixDigits;
    }

    String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String pin = String.valueOf(randomSixDigits.getAsInt());
            boolean taken = games.existsByPinAndStatusNot(pin, GameStatus.FINISHED)
                    || games.existsByPinAndCreatedAtAfter(pin, clock.instant().minus(REUSE_COOLDOWN));
            if (!taken) {
                return pin;
            }
        }
        throw new BusinessRuleException("No se pudo generar un PIN libre. Intenta de nuevo.");
    }

    private static IntSupplier secureSixDigits() {
        SecureRandom random = new SecureRandom();
        return () -> 100_000 + random.nextInt(900_000);
    }
}

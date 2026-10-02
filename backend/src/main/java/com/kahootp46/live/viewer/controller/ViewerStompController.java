package com.kahootp46.live.viewer.controller;

import java.time.Clock;
import java.time.Instant;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.kahootp46.live.game.LiveEvent;
import com.kahootp46.live.viewer.provider.QuizProvider;

/**
 * SUBSCRIBE /app/games/{pin}/state answers once, only to the subscriber, with GAME_CONNECTED + snapshot.
 * The client subscribes again after every reconnection, so it always resynchronizes its state.
 */
@Controller
class ViewerStompController {

    private final QuizProvider provider;
    private final Clock clock;

    ViewerStompController(QuizProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    @SubscribeMapping("/games/{pin}/state")
    LiveEvent state(@DestinationVariable String pin) {
        Instant now = clock.instant();
        return provider.connect(pin)
                .<LiveEvent>map(snapshot -> new LiveEvent.GameConnected(snapshot.gameId(), pin, snapshot.lastSeq(),
                        now, snapshot))
                .orElseGet(() -> new LiveEvent.GameError(null, pin, 0, now, "GAME_NOT_FOUND",
                        "No existe una partida con ese PIN"));
    }
}

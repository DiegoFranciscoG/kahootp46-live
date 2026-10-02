package com.kahootp46.live.game.service;

import java.util.function.LongFunction;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.kahootp46.live.game.LiveEvent;
import com.kahootp46.live.game.domain.Game;

import tools.jackson.databind.json.JsonMapper;

/**
 * Assigns the next sequence number, appends the event to {@code game_events} and publishes it in-process.
 * Listeners that broadcast it run AFTER_COMMIT, so viewers never see an event whose transaction rolled back.
 */
@Component
class LiveEventRecorder {

    private static final String INSERT_SQL =
            "INSERT INTO game_events (game_id, seq, type, payload) VALUES (?, ?, ?, CAST(? AS jsonb))";

    private final JdbcTemplate jdbc;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher publisher;

    LiveEventRecorder(JdbcTemplate jdbc, JsonMapper jsonMapper, ApplicationEventPublisher publisher) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
    }

    void record(Game game, LongFunction<LiveEvent> eventForSeq) {
        LiveEvent event = eventForSeq.apply(game.nextSeq());
        jdbc.update(INSERT_SQL, game.getId(), event.seq(), event.type().name(), jsonMapper.writeValueAsString(event));
        publisher.publishEvent(event);
    }
}

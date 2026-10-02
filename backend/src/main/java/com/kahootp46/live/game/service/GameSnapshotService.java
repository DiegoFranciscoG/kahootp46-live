package com.kahootp46.live.game.service;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kahootp46.live.game.GameViews.GameSnapshot;
import com.kahootp46.live.game.LiveEvent;
import com.kahootp46.live.game.LiveGameQueries;
import com.kahootp46.live.game.repository.GameRepository;
import com.kahootp46.live.quiz.QuizCatalog;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Snapshot read model cached in Redis so every backend instance (and every reconnecting viewer) reads the
 * same state without hitting PostgreSQL. After each committed event the snapshot is rebuilt and written
 * unconditionally; readers only populate the cache when it is empty, so a slow reader cannot overwrite a
 * newer snapshot with an older one.
 */
@Service
class GameSnapshotService implements LiveGameQueries {

    private static final String KEY_PREFIX = "kp46:snapshot:";

    private final GameRepository games;
    private final QuizCatalog quizzes;
    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final Duration ttl;

    GameSnapshotService(GameRepository games, QuizCatalog quizzes, StringRedisTemplate redis, JsonMapper jsonMapper,
            Clock clock, @Value("${app.game.snapshot-ttl}") Duration ttl) {
        this.games = games;
        this.quizzes = quizzes;
        this.redis = redis;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public Optional<GameSnapshot> findSnapshot(String pin) {
        String cached = redis.opsForValue().get(KEY_PREFIX + pin);
        if (cached != null) {
            try {
                return Optional.of(jsonMapper.readValue(cached, GameSnapshot.class).at(clock.instant()));
            } catch (JacksonException ex) {
                redis.delete(KEY_PREFIX + pin);
            }
        }
        Optional<GameSnapshot> built = build(pin);
        built.ifPresent(snapshot -> redis.opsForValue()
                .setIfAbsent(KEY_PREFIX + pin, jsonMapper.writeValueAsString(snapshot), ttl));
        return built;
    }

    @Order(0)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void refresh(LiveEvent event) {
        build(event.pin()).ifPresentOrElse(
                snapshot -> redis.opsForValue().set(KEY_PREFIX + event.pin(), jsonMapper.writeValueAsString(snapshot), ttl),
                () -> redis.delete(KEY_PREFIX + event.pin()));
    }

    private Optional<GameSnapshot> build(String pin) {
        return games.findFirstByPinOrderByCreatedAtDesc(pin)
                .flatMap(game -> quizzes.findPlayable(game.getQuizId())
                        .map(quiz -> GameProjections.snapshot(game, quiz, clock.instant())));
    }
}

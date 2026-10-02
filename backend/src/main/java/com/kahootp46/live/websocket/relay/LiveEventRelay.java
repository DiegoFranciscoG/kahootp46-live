package com.kahootp46.live.websocket.relay;

import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kahootp46.live.game.LiveEvent;

import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes every committed game event to a Redis channel. Each backend instance (including this one)
 * receives it in {@link RedisLiveEventSubscriber} and forwards it to its own WebSocket sessions.
 */
@Component
class LiveEventRelay {

    static final String CHANNEL = "kp46:live-events";

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    LiveEventRelay(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    @Order(10)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void relay(LiveEvent event) {
        redis.convertAndSend(CHANNEL, jsonMapper.writeValueAsString(event));
    }
}

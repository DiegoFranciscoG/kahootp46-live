package com.kahootp46.live.websocket.relay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.kahootp46.live.websocket.LiveTopics;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis Pub/Sub is at-most-once: a viewer that misses an event recovers through the GAME_CONNECTED
 * snapshot on reconnection and through the {@code seq} gap check on the client.
 */
@Component
class RedisLiveEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisLiveEventSubscriber.class);

    private final SimpMessagingTemplate messaging;
    private final JsonMapper jsonMapper;

    RedisLiveEventSubscriber(SimpMessagingTemplate messaging, JsonMapper jsonMapper) {
        this.messaging = messaging;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode event = jsonMapper.readTree(message.getBody());
            String pin = event.path("pin").asString();
            if (pin.matches("\\d{6}")) {
                messaging.convertAndSend(LiveTopics.gameTopic(pin), event);
            }
        } catch (JacksonException ex) {
            log.warn("Discarding malformed live event from Redis");
        }
    }
}

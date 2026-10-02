package com.kahootp46.live.websocket;

/**
 * STOMP destinations of the live protocol.
 */
public final class LiveTopics {

    /** Broadcast of every game event. */
    public static final String GAME_TOPIC_PREFIX = "/topic/games/";
    /** Application prefix handled by {@code @SubscribeMapping} controllers. */
    public static final String APP_PREFIX = "/app";
    /** One-shot subscription that answers GAME_CONNECTED with the current snapshot. */
    public static final String STATE_SUFFIX = "/state";

    private LiveTopics() {
    }

    public static String gameTopic(String pin) {
        return GAME_TOPIC_PREFIX + pin;
    }
}

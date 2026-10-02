package com.kahootp46.live.support;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A viewer as the browser sees it: STOMP CONNECT with the viewer token, then subscribe to the game topic
 * and to the one-shot state destination.
 */
public final class StompTestClient implements AutoCloseable {

    private static final Duration WAIT = Duration.ofSeconds(10);

    private final WebSocketStompClient client;
    private final JsonMapper jsonMapper;
    private final BlockingQueue<JsonNode> events = new LinkedBlockingQueue<>();
    private final CompletableFuture<String> error = new CompletableFuture<>();
    private StompSession session;

    private StompTestClient(JsonMapper jsonMapper) {
        this.client = new WebSocketStompClient(new StandardWebSocketClient());
        this.jsonMapper = jsonMapper;
    }

    public static StompTestClient connect(int port, String token, JsonMapper jsonMapper) throws Exception {
        StompTestClient viewer = new StompTestClient(jsonMapper);
        viewer.session = viewer.connectFuture(port, token).get(WAIT.toSeconds(), TimeUnit.SECONDS);
        return viewer;
    }

    /** Connects without waiting for CONNECTED; used to assert that the server rejects the frame. */
    public static StompTestClient connectExpectingError(int port, String token, JsonMapper jsonMapper) {
        StompTestClient viewer = new StompTestClient(jsonMapper);
        viewer.connectFuture(port, token).thenAccept(session -> viewer.session = session);
        return viewer;
    }

    private CompletableFuture<StompSession> connectFuture(int port, String token) {
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders,
                new SessionHandler());
    }

    /** Subscribes to the broadcast topic and asks for the snapshot, like the Angular client does. */
    public JsonNode watch(String pin) throws InterruptedException {
        subscribe("/topic/games/" + pin);
        subscribe("/app/games/" + pin + "/state");
        JsonNode connected = next("GAME_CONNECTED");
        // The simple broker registers the topic subscription asynchronously.
        Thread.sleep(300);
        return connected;
    }

    public void subscribe(String destination) {
        session.subscribe(destination, new FrameHandler());
    }

    /** Next event of the given type, skipping others; fails after 10 s. */
    public JsonNode next(String type) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode event = events.poll(100, TimeUnit.MILLISECONDS);
            if (event != null && type.equals(event.path("type").asString())) {
                return event;
            }
        }
        throw new AssertionError("Did not receive " + type + " within " + WAIT);
    }

    /** All events received so far, in arrival order. */
    public List<JsonNode> drain() {
        List<JsonNode> drained = new ArrayList<>();
        events.drainTo(drained);
        return drained;
    }

    public String awaitError() throws Exception {
        return error.get(WAIT.toSeconds(), TimeUnit.SECONDS);
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    @Override
    public void close() {
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (RuntimeException alreadyClosedByServer) {
                // The server closes the connection after an ERROR frame.
            }
        }
        client.stop();
    }

    private final class SessionHandler extends StompSessionHandlerAdapter {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            // Only ERROR frames reach the session handler.
            error.complete(headers.getFirst("message"));
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            error.complete("transport: " + exception.getMessage());
        }
    }

    private final class FrameHandler implements StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            events.add(jsonMapper.readTree((byte[]) payload));
        }
    }
}

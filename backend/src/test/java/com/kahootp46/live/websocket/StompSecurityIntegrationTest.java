package com.kahootp46.live.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;
import com.kahootp46.live.support.StompTestClient;

class StompSecurityIntegrationTest extends IntegrationTestSupport {

    @Test
    void connectWithoutTokenIsRejected() throws Exception {
        try (StompTestClient viewer = StompTestClient.connectExpectingError(port, null, json)) {
            assertThat(viewer.awaitError()).isNotBlank();
            assertThat(viewer.isConnected()).isFalse();
        }
    }

    @Test
    void connectWithForgedTokenIsRejected() throws Exception {
        try (StompTestClient viewer = StompTestClient.connectExpectingError(port, "eyJhbGciOiJIUzI1NiJ9.e30.forged", json)) {
            assertThat(viewer.awaitError()).isNotBlank();
        }
    }

    @Test
    void viewerCannotSubscribeToAnotherGame() throws Exception {
        String pinA = createGame(createQuiz("STOMP A", 1, 20).path("id").asString()).path("pin").asString();
        String pinB = createGame(createQuiz("STOMP B", 1, 20).path("id").asString()).path("pin").asString();

        try (StompTestClient viewer = viewer(viewerToken(pinA))) {
            viewer.subscribe("/topic/games/" + pinB);
            assertThat(viewer.awaitError()).isNotBlank();
        }
    }

    @Test
    void viewerCannotSubscribeToArbitraryDestinations() throws Exception {
        String pin = createGame(createQuiz("STOMP C", 1, 20).path("id").asString()).path("pin").asString();

        try (StompTestClient viewer = viewer(viewerToken(pin))) {
            viewer.subscribe("/topic/games");
            assertThat(viewer.awaitError()).isNotBlank();
        }
    }
}

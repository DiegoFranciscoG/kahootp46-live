package com.kahootp46.live.viewer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;
import com.kahootp46.live.support.StompTestClient;

import tools.jackson.databind.JsonNode;

/**
 * A viewer that loses its connection gets everything it missed from the GAME_CONNECTED snapshot.
 */
class ReconnectionIntegrationTest extends IntegrationTestSupport {

    @Test
    void reconnectingViewerRecoversCurrentQuestionAnswerAndHistory() throws Exception {
        JsonNode game = createGame(createQuiz("Reconexión", 3, 60).path("id").asString());
        String pin = game.path("pin").asString();
        String gameId = game.path("id").asString();
        String token = viewerToken(pin);

        long lastSeqSeen;
        try (StompTestClient viewer = viewer(token)) {
            viewer.watch(pin);
            host(gameId, "start");
            lastSeqSeen = viewer.next("QUESTION_STARTED").path("seq").asLong();
        }

        // While disconnected: question 1 revealed and closed, question 2 opened and revealed.
        host(gameId, "reveal");
        host(gameId, "next");
        host(gameId, "reveal");

        try (StompTestClient reconnected = viewer(token)) {
            JsonNode connected = reconnected.watch(pin);
            JsonNode snapshot = connected.path("snapshot");

            assertThat(connected.path("seq").asLong()).isGreaterThan(lastSeqSeen);
            assertThat(snapshot.path("status").asString()).isEqualTo("ANSWER_REVEALED");
            assertThat(snapshot.path("currentQuestion").path("questionNumber").asInt()).isEqualTo(2);
            assertThat(snapshot.path("currentAnswer").path("correctIndex").asInt()).isEqualTo(2);
            assertThat(snapshot.path("history")).hasSize(2);
            assertThat(snapshot.path("history").get(0).path("correctIndex").asInt()).isEqualTo(1);

            // And it keeps receiving live events afterwards.
            host(gameId, "next");
            assertThat(reconnected.next("QUESTION_STARTED").path("questionNumber").asInt()).isEqualTo(3);
        }
    }
}

package com.kahootp46.live.viewer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;
import com.kahootp46.live.support.StompTestClient;

import tools.jackson.databind.JsonNode;

class MultipleViewersIntegrationTest extends IntegrationTestSupport {

    @Test
    void twoViewersReceiveTheSameEventsInTheSameOrder() throws Exception {
        JsonNode game = createGame(createQuiz("Dos viewers", 2, 60).path("id").asString());
        String pin = game.path("pin").asString();
        String gameId = game.path("id").asString();

        try (StompTestClient first = viewer(viewerToken(pin)); StompTestClient second = viewer(viewerToken(pin))) {
            first.watch(pin);
            second.watch(pin);

            host(gameId, "start");
            host(gameId, "reveal");
            first.next("ANSWER_REVEALED");
            second.next("ANSWER_REVEALED");

            assertThat(first.drain()).isEqualTo(second.drain());
        }
    }

    @Test
    void oneHundredSimultaneousViewersReceiveQuestionAndAnswer() throws Exception {
        JsonNode game = createGame(createQuiz("Cien viewers", 1, 60).path("id").asString());
        String pin = game.path("pin").asString();
        String gameId = game.path("id").asString();
        // A viewer token is not single-use: one browser tab per viewer would normally request its own.
        String token = viewerToken(pin);

        List<StompTestClient> viewers = new ArrayList<>();
        try {
            for (int i = 0; i < 100; i++) {
                StompTestClient viewer = viewer(token);
                viewer.subscribe("/topic/games/" + pin);
                viewers.add(viewer);
            }
            Thread.sleep(1_000);

            host(gameId, "start");
            host(gameId, "reveal");

            for (StompTestClient viewer : viewers) {
                assertThat(viewer.next("QUESTION_STARTED").path("questionNumber").asInt()).isEqualTo(1);
                assertThat(viewer.next("ANSWER_REVEALED").path("correctIndex").asInt()).isEqualTo(1);
            }
        } finally {
            viewers.forEach(StompTestClient::close);
        }
    }
}

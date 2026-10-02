package com.kahootp46.live.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;
import com.kahootp46.live.support.StompTestClient;

import tools.jackson.databind.JsonNode;

class QuestionTimeoutIntegrationTest extends IntegrationTestSupport {

    @Test
    void serverRevealsTheAnswerWhenTimeRunsOut() throws Exception {
        JsonNode game = createGame(createQuiz("Tiempo", 1, 5).path("id").asString());
        String pin = game.path("pin").asString();

        try (StompTestClient viewer = viewer(viewerToken(pin))) {
            viewer.watch(pin);
            host(game.path("id").asString(), "start");

            JsonNode question = viewer.next("QUESTION_STARTED");
            assertThat(question.path("timeLimitSeconds").asInt()).isEqualTo(5);

            JsonNode revealed = viewer.next("ANSWER_REVEALED");
            assertThat(revealed.path("reason").asString()).isEqualTo("TIME_UP");
            assertThat(revealed.path("correctIndex").asInt()).isEqualTo(1);
        }
    }

    @Test
    void hostCannotRevealTwiceOrSkipStart() {
        JsonNode game = createGame(createQuiz("Reglas", 1, 60).path("id").asString());
        String gameId = game.path("id").asString();

        assertThat(host(gameId, "reveal").status()).isEqualTo(409);
        assertThat(host(gameId, "next").status()).isEqualTo(409);
        assertThat(host(gameId, "start").status()).isEqualTo(200);
        assertThat(host(gameId, "start").status()).isEqualTo(409);
        assertThat(host(gameId, "reveal").status()).isEqualTo(200);
        assertThat(host(gameId, "reveal").status()).isEqualTo(409);
        assertThat(host(gameId, "finish").status()).isEqualTo(200);
        assertThat(host(gameId, "finish").status()).isEqualTo(409);
    }
}

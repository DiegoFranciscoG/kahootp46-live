package com.kahootp46.live.viewer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;
import com.kahootp46.live.support.StompTestClient;

import tools.jackson.databind.JsonNode;

/**
 * End-to-end flow of the viewer: create game -> PIN -> viewer connects -> QUESTION_STARTED ->
 * ANSWER_REVEALED -> next question -> GAME_FINISHED, all through real WebSocket/STOMP.
 */
class LiveAnswerViewerIntegrationTest extends IntegrationTestSupport {

    @Test
    void viewerFollowsTheWholeGameInRealTime() throws Exception {
        JsonNode quiz = createQuiz("Flujo completo", 2, 60);
        JsonNode game = createGame(quiz.path("id").asString());
        String gameId = game.path("id").asString();
        String pin = game.path("pin").asString();
        assertThat(pin).matches("\\d{6}");
        assertThat(game.path("status").asString()).isEqualTo("LOBBY");

        try (StompTestClient viewer = viewer(viewerToken(pin))) {
            JsonNode connected = viewer.watch(pin);
            assertThat(connected.path("snapshot").path("status").asString()).isEqualTo("LOBBY");
            assertThat(connected.path("seq").asLong()).isZero();

            host(gameId, "start");

            JsonNode started = viewer.next("GAME_STARTED");
            assertThat(started.path("totalQuestions").asInt()).isEqualTo(2);
            JsonNode question1 = viewer.next("QUESTION_STARTED");
            assertThat(question1.path("questionNumber").asInt()).isEqualTo(1);
            assertThat(question1.path("question").asString()).isEqualTo("Pregunta 1 de Flujo completo");
            assertThat(question1.path("choices")).hasSize(4);
            assertThat(question1.path("choices").get(1).path("text").asString()).isEqualTo("Opción B de P1");
            // The answer is never sent together with an open question.
            assertThat(question1.has("correctIndex")).isFalse();
            assertThat(question1.toString()).doesNotContain("correct");

            host(gameId, "reveal");

            JsonNode reveal1 = viewer.next("ANSWER_REVEALED");
            assertThat(reveal1.path("questionNumber").asInt()).isEqualTo(1);
            assertThat(reveal1.path("correctIndex").asInt()).isEqualTo(1);
            assertThat(reveal1.path("correctText").asString()).isEqualTo("Opción B de P1");
            assertThat(reveal1.path("reason").asString()).isEqualTo("HOST");

            host(gameId, "next");

            assertThat(viewer.next("QUESTION_FINISHED").path("questionNumber").asInt()).isEqualTo(1);
            JsonNode question2 = viewer.next("QUESTION_STARTED");
            assertThat(question2.path("questionNumber").asInt()).isEqualTo(2);

            // "next" on an open question reveals it first, then finishes the game (last question).
            host(gameId, "next");

            JsonNode reveal2 = viewer.next("ANSWER_REVEALED");
            assertThat(reveal2.path("correctIndex").asInt()).isEqualTo(2);
            viewer.next("QUESTION_FINISHED");
            JsonNode finished = viewer.next("GAME_FINISHED");
            assertThat(finished.path("reason").asString()).isEqualTo("COMPLETED");
            assertThat(finished.path("questionsPlayed").asInt()).isEqualTo(2);
            assertThat(finished.path("seq").asLong()).isEqualTo(8);
        }

        HttpResult history = get("/api/games/" + pin + "/history", viewerToken(pin));
        assertThat(history.status()).isEqualTo(200);
        assertThat(history.body()).hasSize(2);
        assertThat(history.body().get(0).path("correctIndex").asInt()).isEqualTo(1);
    }

    @Test
    void restEndpointsExposeTheCurrentQuestionButNotItsAnswerUntilRevealed() {
        JsonNode quiz = createQuiz("REST viewer", 3, 60);
        JsonNode game = createGame(quiz.path("id").asString());
        String gameId = game.path("id").asString();
        String pin = game.path("pin").asString();
        String token = viewerToken(pin);

        assertThat(get("/api/games/" + pin + "/current", token).status()).isEqualTo(204);

        host(gameId, "start");
        HttpResult open = get("/api/games/" + pin + "/current", token);
        assertThat(open.status()).isEqualTo(200);
        assertThat(open.body().path("status").asString()).isEqualTo("QUESTION_OPEN");
        assertThat(open.body().path("question").path("questionNumber").asInt()).isEqualTo(1);
        assertThat(open.body().path("answer").isNull() || open.body().path("answer").isMissingNode()).isTrue();

        HttpResult questions = get("/api/games/" + pin + "/questions", token);
        assertThat(questions.body()).hasSize(1);
        assertThat(questions.body().get(0).path("revealed").asBoolean()).isFalse();
        JsonNode hiddenAnswer = questions.body().get(0).path("correctIndex");
        assertThat(hiddenAnswer.isNull() || hiddenAnswer.isMissingNode()).isTrue();

        host(gameId, "reveal");
        HttpResult revealed = get("/api/games/" + pin + "/current", token);
        assertThat(revealed.body().path("answer").path("correctIndex").asInt()).isEqualTo(1);

        HttpResult snapshot = get("/api/games/" + pin, token);
        assertThat(snapshot.body().path("status").asString()).isEqualTo("ANSWER_REVEALED");
        assertThat(snapshot.body().path("history")).hasSize(1);
        assertThat(snapshot.body().path("totalQuestions").asInt()).isEqualTo(3);
    }
}

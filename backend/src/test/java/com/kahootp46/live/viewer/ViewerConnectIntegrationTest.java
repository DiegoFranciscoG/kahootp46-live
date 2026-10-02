package com.kahootp46.live.viewer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;

import tools.jackson.databind.JsonNode;

class ViewerConnectIntegrationTest extends IntegrationTestSupport {

    @Test
    void connectWithValidPinReturnsTokenAndSnapshot() {
        JsonNode game = createGame(createQuiz("Conexión", 1, 20).path("id").asString());
        String pin = game.path("pin").asString();

        HttpResult connected = post("/api/games/connect", Map.of("pin", pin), null);

        assertThat(connected.status()).isEqualTo(200);
        assertThat(connected.body().path("viewerToken").asString()).isNotBlank();
        assertThat(connected.body().path("gameId").asString()).isEqualTo(game.path("id").asString());
        assertThat(connected.body().path("snapshot").path("status").asString()).isEqualTo("LOBBY");
        assertThat(connected.body().path("snapshot").path("quizTitle").asString()).isEqualTo("Conexión");
    }

    @Test
    void invalidPinFormatIsRejected() {
        for (String pin : new String[] {"12345", "1234567", "12a456", ""}) {
            HttpResult result = post("/api/games/connect", Map.of("pin", pin), null);
            assertThat(result.status()).as("pin '%s'", pin).isEqualTo(400);
            assertThat(result.body().path("errors")).as("body %s", result.body()).isNotEmpty();
        }
    }

    @Test
    void unknownPinAnswersNotFound() {
        HttpResult result = post("/api/games/connect", Map.of("pin", "000001"), null);

        assertThat(result.status()).isEqualTo(404);
        assertThat(result.body().path("detail").asString()).isEqualTo("No existe una partida con ese PIN");
    }

    @Test
    void viewerTokenOnlyOpensItsOwnGame() {
        String pinA = createGame(createQuiz("Partida A", 1, 20).path("id").asString()).path("pin").asString();
        String pinB = createGame(createQuiz("Partida B", 1, 20).path("id").asString()).path("pin").asString();
        String tokenA = viewerToken(pinA);

        assertThat(get("/api/games/" + pinA, tokenA).status()).isEqualTo(200);
        assertThat(get("/api/games/" + pinB, tokenA).status()).isEqualTo(403);
        assertThat(get("/api/games/" + pinB + "/history", tokenA).status()).isEqualTo(403);
    }

    @Test
    void readEndpointsRequireAToken() {
        assertThat(get("/api/games/123456", null).status()).isEqualTo(401);
        assertThat(get("/api/quizzes", null).status()).isEqualTo(401);
    }

    @Test
    void viewerTokenCannotUseAdminEndpoints() {
        String pin = createGame(createQuiz("Permisos", 1, 20).path("id").asString()).path("pin").asString();
        String viewerToken = viewerToken(pin);

        assertThat(get("/api/quizzes", viewerToken).status()).isEqualTo(403);
        assertThat(get("/api/host/games", viewerToken).status()).isEqualTo(403);
    }

    @Test
    void connectIsRateLimitedPerClientIp() {
        // Tomcat trusts X-Forwarded-For from internal proxies (localhost here), so each test can use its own IP.
        Map<String, String> fromSameClient = Map.of("X-Forwarded-For", "203.0.113.77");
        int status = 0;
        for (int attempt = 0; attempt < 31; attempt++) {
            status = post("/api/games/connect", Map.of("pin", "000002"), null, fromSameClient).status();
        }
        assertThat(status).isEqualTo(429);

        Map<String, String> otherClient = Map.of("X-Forwarded-For", "203.0.113.78");
        assertThat(post("/api/games/connect", Map.of("pin", "000002"), null, otherClient).status()).isEqualTo(404);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        HttpResult result = post("/api/auth/login", Map.of("email", ADMIN_EMAIL, "password", "not-the-password"), null);

        assertThat(result.status()).isEqualTo(401);
        assertThat(result.body().has("accessToken")).isFalse();
    }
}

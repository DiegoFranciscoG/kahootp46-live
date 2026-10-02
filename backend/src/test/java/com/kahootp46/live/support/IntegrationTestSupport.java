package com.kahootp46.live.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Boots the whole application on a random port against real PostgreSQL and Redis containers.
 * Every integration test shares this configuration, so Spring reuses one context and one pair of containers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String JWT_SECRET = randomSecret(48);
    private static final String ADMIN_PASSWORD = randomSecret(24);
    protected static final String ADMIN_EMAIL = "admin@kahootp46.test";

    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> JWT_SECRET);
        registry.add("ADMIN_PASSWORD", () -> ADMIN_PASSWORD);
    }

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected JsonMapper json;

    private String adminToken;

    protected record HttpResult(int status, JsonNode body) {
    }

    protected String adminPassword() {
        return ADMIN_PASSWORD;
    }

    protected String adminToken() {
        if (adminToken == null) {
            HttpResult login = post("/api/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD), null);
            adminToken = login.body().path("accessToken").asString();
        }
        return adminToken;
    }

    /** Quiz with {@code questions} single-choice questions; question n has its correct answer at index n % 4. */
    protected JsonNode createQuiz(String title, int questions, int timeLimit) {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (int n = 1; n <= questions; n++) {
            int correct = n % 4;
            List<Map<String, Object>> choices = new java.util.ArrayList<>();
            for (int c = 0; c < 4; c++) {
                choices.add(Map.of("text", "Opción " + (char) ('A' + c) + " de P" + n, "correct", c == correct));
            }
            items.add(Map.of("text", "Pregunta " + n + " de " + title, "timeLimit", timeLimit, "choices", choices));
        }
        HttpResult created = post("/api/quizzes", Map.of("title", title, "questions", items), adminToken());
        if (created.status() != 201) {
            throw new AssertionError("Quiz not created: " + created);
        }
        return created.body();
    }

    protected JsonNode createGame(String quizId) {
        HttpResult created = post("/api/host/games", Map.of("quizId", quizId), adminToken());
        if (created.status() != 201) {
            throw new AssertionError("Game not created: " + created);
        }
        return created.body();
    }

    protected HttpResult host(String gameId, String action) {
        return post("/api/host/games/" + gameId + "/" + action, Map.of(), adminToken());
    }

    protected String viewerToken(String pin) {
        // Each simulated viewer comes from its own documentation-range IP so the per-IP limit is not shared.
        String clientIp = "198.51.100." + (1 + RANDOM.nextInt(254));
        HttpResult connected = post("/api/games/connect", Map.of("pin", pin), null, Map.of("X-Forwarded-For", clientIp));
        if (connected.status() != 200) {
            throw new AssertionError("Viewer could not connect: " + connected);
        }
        return connected.body().path("viewerToken").asString();
    }

    protected StompTestClient viewer(String token) throws Exception {
        return StompTestClient.connect(port, token, json);
    }

    protected HttpResult get(String path, String token) {
        return send(request(path, token).GET());
    }

    protected HttpResult post(String path, Object body, String token) {
        return post(path, body, token, Map.of());
    }

    protected HttpResult post(String path, Object body, String token, Map<String, String> headers) {
        HttpRequest.Builder builder = request(path, token)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(json.writeValueAsString(body)));
        headers.forEach(builder::header);
        return send(builder);
    }

    protected HttpResult put(String path, Object body, String token) {
        return send(request(path, token)
                .header("Content-Type", "application/json")
                .PUT(BodyPublishers.ofString(json.writeValueAsString(body))));
    }

    protected HttpResult delete(String path, String token) {
        return send(request(path, token).DELETE());
    }

    protected HttpResult upload(String path, String filename, String contentType, String content,
            Map<String, String> fields, String token) {
        String boundary = "kp46-" + UUID.randomUUID();
        StringBuilder body = new StringBuilder();
        fields.forEach((name, value) -> body.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                .append(value).append("\r\n"));
        body.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n")
                .append("Content-Type: ").append(contentType).append("\r\n\r\n")
                .append(content).append("\r\n")
                .append("--").append(boundary).append("--\r\n");
        BodyPublisher publisher = BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8);
        return send(request(path, token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(publisher));
    }

    private HttpRequest.Builder request(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private HttpResult send(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> response = http.send(builder.build(), BodyHandlers.ofString());
            String body = response.body();
            JsonNode node = body == null || body.isBlank() ? json.createObjectNode() : json.readTree(body);
            return new HttpResult(response.statusCode(), node);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static String randomSecret(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}

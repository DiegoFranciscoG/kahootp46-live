package com.kahootp46.live.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.support.IntegrationTestSupport;

import tools.jackson.databind.JsonNode;

class QuizApiIntegrationTest extends IntegrationTestSupport {

    private static Map<String, Object> question(String text, int correct) {
        return Map.of("text", text, "timeLimit", 20, "choices", List.of(
                Map.of("text", "Uno", "correct", correct == 0),
                Map.of("text", "Dos", "correct", correct == 1)));
    }

    @Test
    void createReadUpdateAndDeleteAQuiz() {
        HttpResult created = post("/api/quizzes",
                Map.of("title", "CRUD", "questions", List.of(question("Primera", 0))), adminToken());
        assertThat(created.status()).isEqualTo(201);
        String id = created.body().path("id").asString();

        HttpResult updated = put("/api/quizzes/" + id, Map.of("title", "CRUD editado", "description", "Con dos preguntas",
                "questions", List.of(question("Primera", 1), question("Segunda", 0))), adminToken());
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.body().path("questions")).hasSize(2);
        assertThat(updated.body().path("questions").get(0).path("choices").get(1).path("correct").asBoolean()).isTrue();

        HttpResult read = get("/api/quizzes/" + id, adminToken());
        assertThat(read.body().path("title").asString()).isEqualTo("CRUD editado");

        HttpResult list = get("/api/quizzes", adminToken());
        assertThat(list.body().findValuesAsString("id")).contains(id);

        assertThat(delete("/api/quizzes/" + id, adminToken()).status()).isEqualTo(204);
        assertThat(get("/api/quizzes/" + id, adminToken()).status()).isEqualTo(404);
    }

    @Test
    void invalidQuizIsRejectedWithDetails() {
        HttpResult result = post("/api/quizzes",
                Map.of("title", "Sin correcta", "questions", List.of(question("P", -1))), adminToken());

        assertThat(result.status()).isEqualTo(400);
        assertThat(result.body().path("errors").toString()).contains("exactamente una opción correcta");
    }

    @Test
    void importsCsvFile() {
        String csv = "question,choice_a,choice_b,choice_c,choice_d,correct,time_limit\n"
                + "¿Cuál es la capital del Ecuador?,Guayaquil,Quito,Cuenca,Loja,B,20\n";

        HttpResult result = upload("/api/quizzes/import", "quiz.csv", "text/csv", csv, Map.of("title", "Desde CSV"),
                adminToken());

        assertThat(result.status()).isEqualTo(201);
        assertThat(result.body().path("title").asString()).isEqualTo("Desde CSV");
        assertThat(result.body().path("questions").get(0).path("choices").get(1).path("correct").asBoolean()).isTrue();
    }

    @Test
    void importsJsonFileAndRejectsOtherExtensions() {
        String json = "{\"title\":\"Desde JSON\",\"questions\":[{\"question\":\"¿P?\",\"choices\":"
                + "[{\"text\":\"A\",\"correct\":true},{\"text\":\"B\",\"correct\":false}]}]}";

        HttpResult imported = upload("/api/quizzes/import", "quiz.json", "application/json", json, Map.of(), adminToken());
        assertThat(imported.status()).isEqualTo(201);

        HttpResult rejected = upload("/api/quizzes/import", "quiz.exe", "application/octet-stream", json, Map.of(),
                adminToken());
        assertThat(rejected.status()).isEqualTo(400);
    }

    @Test
    void quizCannotBeEditedWhileItIsBeingPlayed() {
        JsonNode quiz = createQuiz("En juego", 1, 20);
        String quizId = quiz.path("id").asString();
        JsonNode game = createGame(quizId);

        HttpResult blocked = put("/api/quizzes/" + quizId,
                Map.of("title", "Cambio", "questions", List.of(question("P", 0))), adminToken());
        assertThat(blocked.status()).isEqualTo(409);
        assertThat(delete("/api/quizzes/" + quizId, adminToken()).status()).isEqualTo(409);

        host(game.path("id").asString(), "finish");
        assertThat(put("/api/quizzes/" + quizId,
                Map.of("title", "Cambio", "questions", List.of(question("P", 0))), adminToken()).status()).isEqualTo(200);
    }
}

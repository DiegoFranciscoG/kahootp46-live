package com.kahootp46.live.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.game.GameViews.ChoiceView;
import com.kahootp46.live.game.LiveEvent.RevealReason;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wire format of the protocol (docs/protocolo-websocket.md).
 */
class LiveEventJsonTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final UUID gameId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void questionStartedMatchesTheContractAndHasNoAnswer() {
        LiveEvent event = new LiveEvent.QuestionStarted(gameId, "582941", 2, now, 4, 20,
                "¿Cuál es la capital del Ecuador?",
                List.of(new ChoiceView(0, "Guayaquil"), new ChoiceView(1, "Quito"),
                        new ChoiceView(2, "Cuenca"), new ChoiceView(3, "Loja")),
                20, now, now.plusSeconds(20));

        JsonNode node = json.readTree(json.writeValueAsString(event));

        assertThat(node.path("type").asString()).isEqualTo("QUESTION_STARTED");
        assertThat(node.path("protocolVersion").asInt()).isEqualTo(1);
        assertThat(node.path("gameId").asString()).isEqualTo(gameId.toString());
        assertThat(node.path("questionNumber").asInt()).isEqualTo(4);
        assertThat(node.path("totalQuestions").asInt()).isEqualTo(20);
        assertThat(node.path("choices").get(1).path("index").asInt()).isEqualTo(1);
        assertThat(node.path("choices").get(1).path("text").asString()).isEqualTo("Quito");
        assertThat(node.path("endsAt").asString()).isEqualTo("2026-10-01T12:00:20Z");
        assertThat(node.has("correctIndex")).isFalse();
    }

    @Test
    void answerRevealedCarriesTheCorrectIndex() {
        LiveEvent event = new LiveEvent.AnswerRevealed(gameId, "582941", 3, now, 4, 1, "Quito", RevealReason.TIME_UP);

        JsonNode node = json.readTree(json.writeValueAsString(event));

        assertThat(node.path("type").asString()).isEqualTo("ANSWER_REVEALED");
        assertThat(node.path("correctIndex").asInt()).isEqualTo(1);
        assertThat(node.path("correctText").asString()).isEqualTo("Quito");
        assertThat(node.path("reason").asString()).isEqualTo("TIME_UP");
        assertThat(node.path("seq").asLong()).isEqualTo(3);
    }
}

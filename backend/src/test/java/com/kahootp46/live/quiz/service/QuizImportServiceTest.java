package com.kahootp46.live.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.quiz.QuestionType;
import com.kahootp46.live.quiz.dto.QuizRequest;
import com.kahootp46.live.shared.error.InvalidInputException;

import tools.jackson.databind.json.JsonMapper;

class QuizImportServiceTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final QuizRules rules = new QuizRules(validator);
    private final QuizImportService importer =
            new QuizImportService(mock(QuizService.class), JsonMapper.builder().build());

    @Test
    void importsTheDocumentedJsonFormat() {
        String json = """
                {
                  "title": "Historia del Ecuador",
                  "questions": [
                    {
                      "question": "¿Cuál es la capital del Ecuador?",
                      "choices": [
                        { "text": "Guayaquil", "correct": false },
                        { "text": "Quito", "correct": true }
                      ]
                    }
                  ]
                }
                """;

        QuizRequest request = importer.fromJson(json);

        assertThat(request.title()).isEqualTo("Historia del Ecuador");
        assertThat(request.questions()).hasSize(1);
        assertThat(request.questions().getFirst().timeLimit()).isEqualTo(QuizRules.DEFAULT_TIME_LIMIT);
        assertThat(request.questions().getFirst().choices().get(1).correct()).isTrue();
        rules.validate(request);
    }

    @Test
    void malformedJsonIsAValidationError() {
        assertThatThrownBy(() -> importer.fromJson("{ not json")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> importer.fromJson("{\"title\": \"x\"}")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void importsCsvWithOptionalColumns() {
        String csv = """
                question,choice_a,choice_b,choice_c,choice_d,correct,time_limit
                ¿Cuál es la capital del Ecuador?,Guayaquil,Quito,Cuenca,Loja,B,30
                "Pregunta con coma, entre comillas",Sí,No,,,a,
                Galápagos pertenece a Ecuador,Verdadero,Falso,,,A,10
                """;

        QuizRequest request = importer.fromCsv(csv, "Importado");

        assertThat(request.title()).isEqualTo("Importado");
        assertThat(request.questions()).hasSize(3);
        assertThat(request.questions().get(0).choices()).hasSize(4);
        assertThat(request.questions().get(0).choices().get(1).correct()).isTrue();
        assertThat(request.questions().get(0).timeLimit()).isEqualTo(30);
        assertThat(request.questions().get(1).text()).isEqualTo("Pregunta con coma, entre comillas");
        assertThat(request.questions().get(1).choices()).hasSize(2);
        assertThat(request.questions().get(1).timeLimit()).isEqualTo(QuizRules.DEFAULT_TIME_LIMIT);
        assertThat(request.questions().get(2).typeOrDefault()).isEqualTo(QuestionType.TRUE_FALSE);
        rules.validate(request);
    }

    @Test
    void csvRequiresTitleHeaderAndValidCorrectLetter() {
        String valid = "question,choice_a,choice_b,correct\nP,A,B,A\n";
        assertThatThrownBy(() -> importer.fromCsv(valid, " ")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> importer.fromCsv("pregunta,a,b\nP,A,B\n", "T"))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> importer.fromCsv("question,choice_a,choice_b,correct\nP,A,B,D\n", "T"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Fila 1");
    }
}

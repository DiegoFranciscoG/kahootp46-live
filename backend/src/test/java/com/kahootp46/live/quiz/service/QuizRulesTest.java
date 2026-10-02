package com.kahootp46.live.quiz.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import com.kahootp46.live.quiz.QuestionType;
import com.kahootp46.live.quiz.dto.ChoiceRequest;
import com.kahootp46.live.quiz.dto.QuestionRequest;
import com.kahootp46.live.quiz.dto.QuizRequest;
import com.kahootp46.live.shared.error.InvalidInputException;

class QuizRulesTest {

    private final QuizRules rules = new QuizRules(Validation.buildDefaultValidatorFactory().getValidator());

    private static QuizRequest quiz(QuestionRequest... questions) {
        return new QuizRequest("Quiz", null, List.of(questions));
    }

    private static QuestionRequest question(QuestionType type, int timeLimit, ChoiceRequest... choices) {
        return new QuestionRequest("¿Pregunta?", type, timeLimit, List.of(choices));
    }

    private static ChoiceRequest ok(String text) {
        return new ChoiceRequest(text, true);
    }

    private static ChoiceRequest no(String text) {
        return new ChoiceRequest(text, false);
    }

    @Test
    void acceptsAValidQuiz() {
        assertThatCode(() -> rules.validate(quiz(
                question(QuestionType.SINGLE_CHOICE, 20, no("A"), ok("B"), no("C"), no("D")),
                question(QuestionType.TRUE_FALSE, 10, ok("Verdadero"), no("Falso")))))
                .doesNotThrowAnyException();
    }

    @Test
    void requiresExactlyOneCorrectChoice() {
        assertThatThrownBy(() -> rules.validate(quiz(question(null, 20, no("A"), no("B")))))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("exactamente una opción correcta");
        assertThatThrownBy(() -> rules.validate(quiz(question(null, 20, ok("A"), ok("B")))))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsUnsupportedTimeLimitsAndChoiceCounts() {
        assertThatThrownBy(() -> rules.validate(quiz(question(null, 7, ok("A"), no("B")))))
                .hasMessageContaining("tiempo");
        assertThatThrownBy(() -> rules.validate(quiz(question(null, 20, ok("A")))))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> rules.validate(quiz(question(QuestionType.TRUE_FALSE, 20, ok("A"), no("B"), no("C")))))
                .hasMessageContaining("verdadero/falso");
    }

    @Test
    void rejectsEmptyQuizAndBlankTexts() {
        assertThatThrownBy(() -> rules.validate(new QuizRequest("Quiz", null, List.of())))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> rules.validate(new QuizRequest(" ", null,
                List.of(question(null, 20, ok("A"), no("B"))))))
                .isInstanceOf(InvalidInputException.class);
    }
}

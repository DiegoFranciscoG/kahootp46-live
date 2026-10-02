package com.kahootp46.live.quiz.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import com.kahootp46.live.quiz.QuestionType;
import com.kahootp46.live.quiz.dto.ChoiceRequest;
import com.kahootp46.live.quiz.dto.QuestionRequest;
import com.kahootp46.live.quiz.dto.QuizRequest;
import com.kahootp46.live.shared.error.InvalidInputException;

/**
 * Single validation path for quizzes created by form, edited or imported from JSON/CSV.
 */
@Component
public class QuizRules {

    /** Allowed time limits in seconds (same presets offered by the admin UI). */
    public static final Set<Integer> TIME_LIMITS = Set.of(5, 10, 20, 30, 60, 90, 120, 240);
    public static final int DEFAULT_TIME_LIMIT = 20;

    private final Validator validator;

    QuizRules(Validator validator) {
        this.validator = validator;
    }

    public void validate(QuizRequest request) {
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<QuizRequest> violation : validator.validate(request)) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        if (errors.isEmpty()) {
            for (int i = 0; i < request.questions().size(); i++) {
                validateQuestion(i + 1, request.questions().get(i), errors);
            }
        }
        if (!errors.isEmpty()) {
            throw new InvalidInputException(errors);
        }
    }

    private static void validateQuestion(int number, QuestionRequest question, List<String> errors) {
        String prefix = "Pregunta " + number + ": ";
        if (!TIME_LIMITS.contains(question.timeLimit())) {
            errors.add(prefix + "el tiempo debe ser uno de " + TIME_LIMITS.stream().sorted().toList() + " segundos");
        }
        if (question.typeOrDefault() == QuestionType.TRUE_FALSE && question.choices().size() != 2) {
            errors.add(prefix + "una pregunta verdadero/falso debe tener exactamente 2 opciones");
        }
        long correct = question.choices().stream().filter(ChoiceRequest::correct).count();
        if (correct != 1) {
            errors.add(prefix + "debe tener exactamente una opción correcta (tiene " + correct + ")");
        }
    }
}

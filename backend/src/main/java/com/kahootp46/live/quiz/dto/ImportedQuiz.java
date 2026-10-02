package com.kahootp46.live.quiz.dto;

import java.util.List;

import com.kahootp46.live.quiz.QuestionType;

/**
 * JSON import format (see docs/protocolo-websocket.md#importacion):
 * <pre>{"title": "...", "questions": [{"question": "...", "timeLimit": 20, "choices": [{"text": "...", "correct": true}]}]}</pre>
 */
public record ImportedQuiz(String title, String description, List<ImportedQuestion> questions) {

    public record ImportedQuestion(String question, QuestionType type, Integer timeLimit, List<ChoiceRequest> choices) {
    }
}

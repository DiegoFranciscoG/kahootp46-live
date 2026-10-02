package com.kahootp46.live.quiz.mapper;

import java.util.List;

import com.kahootp46.live.quiz.PlayableChoice;
import com.kahootp46.live.quiz.PlayableQuestion;
import com.kahootp46.live.quiz.PlayableQuiz;
import com.kahootp46.live.quiz.domain.Choice;
import com.kahootp46.live.quiz.domain.Question;
import com.kahootp46.live.quiz.domain.Quiz;
import com.kahootp46.live.quiz.dto.QuizResponse;
import com.kahootp46.live.quiz.dto.QuizResponse.ChoiceResponse;
import com.kahootp46.live.quiz.dto.QuizResponse.QuestionResponse;
import com.kahootp46.live.quiz.dto.QuizSummaryResponse;

public final class QuizMapper {

    private QuizMapper() {
    }

    public static QuizResponse toResponse(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(QuizMapper::toResponse)
                .toList();
        return new QuizResponse(quiz.getId(), quiz.getTitle(), quiz.getDescription(), quiz.getCreatedAt(),
                quiz.getUpdatedAt(), questions);
    }

    public static QuizSummaryResponse toSummary(Quiz quiz) {
        return new QuizSummaryResponse(quiz.getId(), quiz.getTitle(), quiz.getDescription(),
                quiz.getQuestions().size(), quiz.getUpdatedAt());
    }

    public static PlayableQuiz toPlayable(Quiz quiz) {
        List<PlayableQuestion> questions = quiz.getQuestions().stream()
                .map(QuizMapper::toPlayable)
                .toList();
        return new PlayableQuiz(quiz.getId(), quiz.getOwnerId(), quiz.getTitle(), questions);
    }

    private static QuestionResponse toResponse(Question question) {
        List<ChoiceResponse> choices = question.getChoices().stream()
                .map(choice -> new ChoiceResponse(choice.getId(), choice.getOrderIndex(), choice.getText(),
                        choice.isCorrect()))
                .toList();
        return new QuestionResponse(question.getId(), question.getOrderIndex() + 1, question.getText(),
                question.getType(), question.getTimeLimit(), choices);
    }

    private static PlayableQuestion toPlayable(Question question) {
        List<PlayableChoice> choices = question.getChoices().stream()
                .map(QuizMapper::toPlayable)
                .toList();
        return new PlayableQuestion(question.getOrderIndex() + 1, question.getText(), question.getType(),
                question.getTimeLimit(), choices);
    }

    private static PlayableChoice toPlayable(Choice choice) {
        return new PlayableChoice(choice.getOrderIndex(), choice.getText(), choice.isCorrect());
    }
}

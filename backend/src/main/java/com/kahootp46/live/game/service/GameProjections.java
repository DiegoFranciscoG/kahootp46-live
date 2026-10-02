package com.kahootp46.live.game.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.GameViews.ChoiceView;
import com.kahootp46.live.game.GameViews.GameSnapshot;
import com.kahootp46.live.game.GameViews.HistoryItem;
import com.kahootp46.live.game.GameViews.LiveQuestion;
import com.kahootp46.live.game.GameViews.RevealedAnswer;
import com.kahootp46.live.game.LiveEvent;
import com.kahootp46.live.game.LiveEvent.FinishReason;
import com.kahootp46.live.game.LiveEvent.RevealReason;
import com.kahootp46.live.game.domain.Game;
import com.kahootp46.live.quiz.PlayableChoice;
import com.kahootp46.live.quiz.PlayableQuestion;
import com.kahootp46.live.quiz.PlayableQuiz;

/**
 * Builds protocol events and snapshots from a game and its quiz. The correct answer only appears in
 * {@link RevealedAnswer}, {@link HistoryItem} and ANSWER_REVEALED, never next to an open question.
 */
final class GameProjections {

    private GameProjections() {
    }

    static GameSnapshot snapshot(Game game, PlayableQuiz quiz, Instant now) {
        List<HistoryItem> history = IntStream.rangeClosed(1, game.revealedCount())
                .mapToObj(number -> historyItem(quiz.question(number)))
                .toList();
        boolean questionOnScreen = game.getStatus() == GameStatus.QUESTION_OPEN
                || game.getStatus() == GameStatus.ANSWER_REVEALED;
        LiveQuestion current = questionOnScreen ? liveQuestion(game, quiz) : null;
        RevealedAnswer answer = game.getStatus() == GameStatus.ANSWER_REVEALED
                ? revealedAnswer(quiz.question(game.getCurrentQuestion()))
                : null;
        return new GameSnapshot(game.getId(), game.getPin(), game.getStatus(), quiz.title(), game.getTotalQuestions(),
                current, answer, history, game.getLastSeq(), now);
    }

    static LiveQuestion liveQuestion(Game game, PlayableQuiz quiz) {
        PlayableQuestion question = quiz.question(game.getCurrentQuestion());
        return new LiveQuestion(question.number(), game.getTotalQuestions(), question.text(), choices(question),
                question.timeLimitSeconds(), game.getQuestionStartedAt(), game.getQuestionEndsAt());
    }

    static RevealedAnswer revealedAnswer(PlayableQuestion question) {
        PlayableChoice correct = question.correctChoice();
        return new RevealedAnswer(question.number(), correct.index(), correct.text());
    }

    static List<ChoiceView> choices(PlayableQuestion question) {
        return question.choices().stream()
                .map(choice -> new ChoiceView(choice.index(), choice.text()))
                .toList();
    }

    static LiveEvent.GameStarted gameStarted(Game game, PlayableQuiz quiz, long seq, Instant now) {
        return new LiveEvent.GameStarted(game.getId(), game.getPin(), seq, now, quiz.title(), game.getTotalQuestions());
    }

    static LiveEvent.QuestionStarted questionStarted(Game game, PlayableQuiz quiz, long seq, Instant now) {
        LiveQuestion question = liveQuestion(game, quiz);
        return new LiveEvent.QuestionStarted(game.getId(), game.getPin(), seq, now, question.questionNumber(),
                question.totalQuestions(), question.question(), question.choices(), question.timeLimitSeconds(),
                question.startedAt(), question.endsAt());
    }

    static LiveEvent.AnswerRevealed answerRevealed(Game game, PlayableQuiz quiz, RevealReason reason, long seq,
            Instant now) {
        RevealedAnswer answer = revealedAnswer(quiz.question(game.getCurrentQuestion()));
        return new LiveEvent.AnswerRevealed(game.getId(), game.getPin(), seq, now, answer.questionNumber(),
                answer.correctIndex(), answer.correctText(), reason);
    }

    static LiveEvent.QuestionFinished questionFinished(Game game, long seq, Instant now) {
        return new LiveEvent.QuestionFinished(game.getId(), game.getPin(), seq, now, game.getCurrentQuestion());
    }

    static LiveEvent.GameFinished gameFinished(Game game, FinishReason reason, long seq, Instant now) {
        return new LiveEvent.GameFinished(game.getId(), game.getPin(), seq, now, game.revealedCount(),
                game.getTotalQuestions(), reason);
    }

    private static HistoryItem historyItem(PlayableQuestion question) {
        RevealedAnswer answer = revealedAnswer(question);
        return new HistoryItem(question.number(), question.text(), choices(question), answer.correctIndex(),
                answer.correctText());
    }
}

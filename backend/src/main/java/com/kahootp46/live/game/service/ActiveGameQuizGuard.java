package com.kahootp46.live.game.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.repository.GameRepository;
import com.kahootp46.live.quiz.QuizUsageGuard;

@Component
class ActiveGameQuizGuard implements QuizUsageGuard {

    private final GameRepository games;

    ActiveGameQuizGuard(GameRepository games) {
        this.games = games;
    }

    @Override
    public boolean isInActiveGame(UUID quizId) {
        return games.existsByQuizIdAndStatusNot(quizId, GameStatus.FINISHED);
    }
}

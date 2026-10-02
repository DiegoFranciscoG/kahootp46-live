package com.kahootp46.live.quiz;

import java.util.UUID;

/**
 * Port implemented by the game module so the quiz module can refuse edits to a quiz that is being played,
 * without depending on the game module (dependency inversion, no cycle).
 */
public interface QuizUsageGuard {

    boolean isInActiveGame(UUID quizId);
}

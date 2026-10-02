package com.kahootp46.live.quiz;

import java.util.Optional;
import java.util.UUID;

/**
 * Read access to quiz content for the game engine.
 */
public interface QuizCatalog {

    Optional<PlayableQuiz> findPlayable(UUID quizId);
}

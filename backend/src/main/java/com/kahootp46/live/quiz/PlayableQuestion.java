package com.kahootp46.live.quiz;

import java.util.List;

/**
 * Immutable question as the game engine needs it. {@code number} is 1-based.
 */
public record PlayableQuestion(int number, String text, QuestionType type, int timeLimitSeconds,
        List<PlayableChoice> choices) {

    public PlayableQuestion {
        choices = List.copyOf(choices);
    }

    public PlayableChoice correctChoice() {
        return choices.stream()
                .filter(PlayableChoice::correct)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question " + number + " has no correct choice"));
    }
}

package com.kahootp46.live.quiz;

import java.util.List;
import java.util.UUID;

public record PlayableQuiz(UUID id, UUID ownerId, String title, List<PlayableQuestion> questions) {

    public PlayableQuiz {
        questions = List.copyOf(questions);
    }

    public int totalQuestions() {
        return questions.size();
    }

    /** @param number 1-based question number */
    public PlayableQuestion question(int number) {
        return questions.get(number - 1);
    }
}

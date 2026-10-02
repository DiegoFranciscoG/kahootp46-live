package com.kahootp46.live.viewer.dto;

import java.util.List;

import com.kahootp46.live.game.GameViews.ChoiceView;

/**
 * A question already shown in the game. Future questions are never exposed; {@code correctIndex} is null
 * while the question is still open.
 */
public record ShownQuestion(int questionNumber, String question, List<ChoiceView> choices, boolean revealed,
        Integer correctIndex) {
}

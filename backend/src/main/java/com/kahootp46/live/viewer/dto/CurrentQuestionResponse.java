package com.kahootp46.live.viewer.dto;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.GameViews.LiveQuestion;
import com.kahootp46.live.game.GameViews.RevealedAnswer;

/**
 * {@code question} is null when no question is on screen; {@code answer} is null until it is revealed.
 */
public record CurrentQuestionResponse(GameStatus status, LiveQuestion question, RevealedAnswer answer) {
}

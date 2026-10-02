package com.kahootp46.live.game;

/**
 * Server-side game state machine: LOBBY -> QUESTION_OPEN -> ANSWER_REVEALED -> (QUESTION_OPEN ...) -> FINISHED.
 */
public enum GameStatus {
    LOBBY,
    QUESTION_OPEN,
    ANSWER_REVEALED,
    FINISHED
}

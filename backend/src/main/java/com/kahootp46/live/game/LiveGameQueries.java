package com.kahootp46.live.game;

import java.util.Optional;

import com.kahootp46.live.game.GameViews.GameSnapshot;

/**
 * Read side of the game engine for viewers. Backed by a Redis cache shared by every backend instance.
 */
public interface LiveGameQueries {

    /** Latest game with this PIN (a PIN is unique among unfinished games and not reused for 24 h). */
    Optional<GameSnapshot> findSnapshot(String pin);
}

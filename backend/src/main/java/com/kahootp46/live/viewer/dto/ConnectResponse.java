package com.kahootp46.live.viewer.dto;

import java.time.Instant;
import java.util.UUID;

import com.kahootp46.live.game.GameViews.GameSnapshot;

/**
 * The viewer token is bound to this PIN and is required by the REST read endpoints and by the STOMP CONNECT frame.
 */
public record ConnectResponse(UUID gameId, String pin, String viewerToken, Instant expiresAt, GameSnapshot snapshot) {
}

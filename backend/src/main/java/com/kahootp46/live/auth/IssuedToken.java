package com.kahootp46.live.auth;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {
}

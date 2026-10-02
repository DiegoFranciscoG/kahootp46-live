package com.kahootp46.live.shared.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    /**
     * Counts one hit for {@code key} and fails when more than {@code limit} hits happen inside {@code window}.
     *
     * @throws RateLimitExceededException when the limit is exceeded
     */
    void acquire(String key, int limit, Duration window);
}

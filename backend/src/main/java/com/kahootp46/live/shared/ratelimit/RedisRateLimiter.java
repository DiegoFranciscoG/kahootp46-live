package com.kahootp46.live.shared.ratelimit;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter shared by every backend instance: INCR + PEXPIRE executed atomically in Redis.
 */
@Component
class RedisRateLimiter implements RateLimiter {

    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;

    RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void acquire(String key, int limit, Duration window) {
        Long hits = redis.execute(INCREMENT_SCRIPT, List.of("kp46:rl:" + key), String.valueOf(window.toMillis()));
        if (hits != null && hits > limit) {
            throw new RateLimitExceededException(window);
        }
    }
}

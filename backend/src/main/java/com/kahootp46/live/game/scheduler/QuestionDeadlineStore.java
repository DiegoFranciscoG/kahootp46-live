package com.kahootp46.live.game.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Question deadlines in a Redis sorted set (score = epoch millis). Any instance can pick up a due
 * deadline; ZREM returning 1 is the claim, so exactly one instance fires each timeout.
 */
@Component
class QuestionDeadlineStore {

    private static final String KEY = "kp46:deadlines";

    private final StringRedisTemplate redis;

    QuestionDeadlineStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    void schedule(UUID gameId, int questionNumber, Instant endsAt) {
        redis.opsForZSet().add(KEY, member(gameId, questionNumber), endsAt.toEpochMilli());
    }

    void cancel(UUID gameId, int questionNumber) {
        redis.opsForZSet().remove(KEY, member(gameId, questionNumber));
    }

    List<Deadline> claimDue(Instant now, int max) {
        Set<String> due = redis.opsForZSet().rangeByScore(KEY, 0, now.toEpochMilli(), 0, max);
        List<Deadline> claimed = new ArrayList<>();
        if (due == null) {
            return claimed;
        }
        for (String member : due) {
            Long removed = redis.opsForZSet().remove(KEY, member);
            if (removed != null && removed == 1) {
                claimed.add(parse(member));
            }
        }
        return claimed;
    }

    private static String member(UUID gameId, int questionNumber) {
        return gameId + ":" + questionNumber;
    }

    private static Deadline parse(String member) {
        int separator = member.lastIndexOf(':');
        return new Deadline(UUID.fromString(member.substring(0, separator)),
                Integer.parseInt(member.substring(separator + 1)));
    }

    record Deadline(UUID gameId, int questionNumber) {
    }
}

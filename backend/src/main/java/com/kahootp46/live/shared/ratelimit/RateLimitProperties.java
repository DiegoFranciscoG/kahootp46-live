package com.kahootp46.live.shared.ratelimit;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(@Positive int connectPerMinute, @Positive int loginPerMinute) {
}

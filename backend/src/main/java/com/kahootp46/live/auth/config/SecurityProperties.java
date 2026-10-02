package com.kahootp46.live.auth.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security")
public record SecurityProperties(
        @NotBlank String jwtSecret,
        @NotNull Duration adminTokenTtl,
        @NotNull Duration viewerTokenTtl) {
}

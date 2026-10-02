package com.kahootp46.live.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bootstrap admin account, provided only through environment variables.
 */
@Validated
@ConfigurationProperties("app.admin")
public record AdminProperties(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 72, message = "ADMIN_PASSWORD debe tener entre 12 y 72 caracteres") String password) {
}

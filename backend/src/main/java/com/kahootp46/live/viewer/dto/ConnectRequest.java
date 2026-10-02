package com.kahootp46.live.viewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConnectRequest(
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "El PIN debe tener exactamente 6 dígitos") String pin) {
}

package com.kahootp46.live.viewer.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kahootp46.live.game.GameViews.GameSnapshot;
import com.kahootp46.live.game.GameViews.HistoryItem;
import com.kahootp46.live.viewer.dto.ConnectRequest;
import com.kahootp46.live.viewer.dto.ConnectResponse;
import com.kahootp46.live.viewer.dto.CurrentQuestionResponse;
import com.kahootp46.live.viewer.dto.ShownQuestion;
import com.kahootp46.live.viewer.service.ViewerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Viewer", description = "Read-only access to a live game by PIN")
class ViewerController {

    private static final String PIN_REGEX = "^\\d{6}$";
    private static final String PIN_MESSAGE = "El PIN debe tener exactamente 6 dígitos";

    private final ViewerService viewerService;

    ViewerController(ViewerService viewerService) {
        this.viewerService = viewerService;
    }

    @PostMapping("/connect")
    @Operation(summary = "Validate a PIN and get a viewer token bound to it plus the current snapshot")
    ConnectResponse connect(@Valid @RequestBody ConnectRequest request, HttpServletRequest http) {
        return viewerService.connect(request.pin(), http.getRemoteAddr());
    }

    @GetMapping("/{pin}")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Full snapshot: status, current question, revealed answer and history")
    GameSnapshot game(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = PIN_REGEX, message = PIN_MESSAGE) String pin) {
        return viewerService.snapshot(jwt, pin);
    }

    @GetMapping("/{pin}/current")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Current question and, once revealed, its correct answer")
    ResponseEntity<CurrentQuestionResponse> current(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = PIN_REGEX, message = PIN_MESSAGE) String pin) {
        CurrentQuestionResponse current = viewerService.current(jwt, pin);
        return current.question() == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(current);
    }

    @GetMapping("/{pin}/questions")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Questions shown so far (never future ones)")
    List<ShownQuestion> questions(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = PIN_REGEX, message = PIN_MESSAGE) String pin) {
        return viewerService.questions(jwt, pin);
    }

    @GetMapping("/{pin}/history")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Closed questions with their revealed answer")
    List<HistoryItem> history(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = PIN_REGEX, message = PIN_MESSAGE) String pin) {
        return viewerService.history(jwt, pin);
    }
}

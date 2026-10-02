package com.kahootp46.live.game.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kahootp46.live.game.dto.CreateGameRequest;
import com.kahootp46.live.game.dto.HostGameResponse;
import com.kahootp46.live.game.service.GameService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/host/games")
@Tag(name = "Host", description = "Start, drive and close games of the admin's own quizzes")
@SecurityRequirement(name = "bearer")
class HostGameController {

    private final GameService gameService;

    HostGameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @Operation(summary = "Create a game in LOBBY and generate its 6-digit PIN")
    ResponseEntity<HostGameResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGameRequest request) {
        HostGameResponse game = gameService.create(hostId(jwt), request.quizId());
        return ResponseEntity.created(URI.create("/api/host/games/" + game.id())).body(game);
    }

    @GetMapping
    List<HostGameResponse> recent(@AuthenticationPrincipal Jwt jwt) {
        return gameService.recent(hostId(jwt));
    }

    @GetMapping("/{id}")
    HostGameResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return gameService.get(hostId(jwt), id);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "GAME_STARTED + QUESTION_STARTED for question 1")
    HostGameResponse start(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return gameService.start(hostId(jwt), id);
    }

    @PostMapping("/{id}/reveal")
    @Operation(summary = "ANSWER_REVEALED before the timer ends")
    HostGameResponse reveal(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return gameService.reveal(hostId(jwt), id);
    }

    @PostMapping("/{id}/next")
    @Operation(summary = "QUESTION_FINISHED, then QUESTION_STARTED (next) or GAME_FINISHED (last)")
    HostGameResponse next(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return gameService.next(hostId(jwt), id);
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "Close the game (GAME_FINISHED)")
    HostGameResponse finish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return gameService.finish(hostId(jwt), id);
    }

    private static UUID hostId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

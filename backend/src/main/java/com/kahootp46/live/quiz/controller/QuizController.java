package com.kahootp46.live.quiz.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kahootp46.live.quiz.dto.QuizRequest;
import com.kahootp46.live.quiz.dto.QuizResponse;
import com.kahootp46.live.quiz.dto.QuizSummaryResponse;
import com.kahootp46.live.quiz.service.QuizImportService;
import com.kahootp46.live.quiz.service.QuizService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes", description = "CRUD and import of the admin's own quizzes")
@SecurityRequirement(name = "bearer")
class QuizController {

    private final QuizService quizService;
    private final QuizImportService importService;

    QuizController(QuizService quizService, QuizImportService importService) {
        this.quizService = quizService;
        this.importService = importService;
    }

    @GetMapping
    List<QuizSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return quizService.list(ownerId(jwt));
    }

    @GetMapping("/{id}")
    QuizResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return quizService.get(ownerId(jwt), id);
    }

    @PostMapping
    @Operation(summary = "Create a quiz with its questions, choices, correct answer and time per question")
    ResponseEntity<QuizResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuizRequest request) {
        QuizResponse created = quizService.create(ownerId(jwt), request);
        return ResponseEntity.created(URI.create("/api/quizzes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace title, questions and choices of a quiz (refused while it is being played)")
    QuizResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody QuizRequest request) {
        return quizService.update(ownerId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        quizService.delete(ownerId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import a quiz from a .json or .csv file (max 256 KB, UTF-8)")
    ResponseEntity<QuizResponse> importQuiz(@AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "title", required = false) String title) {
        QuizResponse created = importService.importFile(ownerId(jwt), file, title);
        return ResponseEntity.created(URI.create("/api/quizzes/" + created.id())).body(created);
    }

    private static UUID ownerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

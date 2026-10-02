package com.kahootp46.live.quiz.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.kahootp46.live.auth.AdminAccounts;

/**
 * Loads the fictitious demo quizzes from {@code classpath:demo/*.json} through the regular import path,
 * only when {@code app.demo.seed-enabled=true} and the admin has no quizzes yet.
 */
@Component
@ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true")
class DemoQuizSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoQuizSeeder.class);

    private final AdminAccounts adminAccounts;
    private final QuizService quizService;
    private final QuizImportService importService;

    DemoQuizSeeder(AdminAccounts adminAccounts, QuizService quizService, QuizImportService importService) {
        this.adminAccounts = adminAccounts;
        this.quizService = quizService;
        this.importService = importService;
    }

    @EventListener(ApplicationReadyEvent.class)
    void seed() throws IOException {
        UUID ownerId = adminAccounts.findBootstrapAdminId().orElseThrow();
        if (quizService.hasQuizzes(ownerId)) {
            return;
        }
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:demo/*.json");
        Arrays.sort(files, Comparator.comparing(Resource::getFilename));
        for (Resource file : files) {
            try (InputStream in = file.getInputStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                quizService.create(ownerId, importService.fromJson(json));
            }
        }
        log.info("Demo quizzes loaded: {}", files.length);
    }
}

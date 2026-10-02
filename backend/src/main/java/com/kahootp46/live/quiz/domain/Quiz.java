package com.kahootp46.live.quiz.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.kahootp46.live.quiz.QuestionType;

/**
 * Aggregate root: questions and choices are only created or replaced through the quiz.
 */
@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Question> questions = new ArrayList<>();

    protected Quiz() {
    }

    public static Quiz create(UUID ownerId, String title, String description, Instant now) {
        Quiz quiz = new Quiz();
        quiz.id = UUID.randomUUID();
        quiz.ownerId = ownerId;
        quiz.createdAt = now;
        quiz.describe(title, description, now);
        return quiz;
    }

    public void describe(String title, String description, Instant now) {
        this.title = title.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.updatedAt = now;
    }

    public Question addQuestion(String text, QuestionType type, int timeLimit) {
        Question question = new Question(this, text, type, timeLimit, questions.size());
        questions.add(question);
        return question;
    }

    public void clearQuestions() {
        questions.clear();
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}

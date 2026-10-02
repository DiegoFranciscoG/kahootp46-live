package com.kahootp46.live.quiz.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.kahootp46.live.quiz.QuestionType;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(name = "time_limit", nullable = false)
    private int timeLimit;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Choice> choices = new ArrayList<>();

    protected Question() {
    }

    Question(Quiz quiz, String text, QuestionType type, int timeLimit, int orderIndex) {
        this.id = UUID.randomUUID();
        this.quiz = quiz;
        this.text = text.strip();
        this.type = type;
        this.timeLimit = timeLimit;
        this.orderIndex = orderIndex;
    }

    public void addChoice(String text, boolean correct) {
        choices.add(new Choice(this, text, correct, choices.size()));
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public QuestionType getType() {
        return type;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public List<Choice> getChoices() {
        return Collections.unmodifiableList(choices);
    }
}

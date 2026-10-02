package com.kahootp46.live.quiz.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kahootp46.live.quiz.domain.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);
}

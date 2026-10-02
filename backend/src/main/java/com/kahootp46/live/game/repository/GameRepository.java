package com.kahootp46.live.game.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kahootp46.live.game.GameStatus;
import com.kahootp46.live.game.domain.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {

    /** Row lock: host commands and timeouts on the same game are serialized, even across instances. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.id = :id")
    Optional<Game> findForUpdate(@Param("id") UUID id);

    Optional<Game> findFirstByPinOrderByCreatedAtDesc(String pin);

    List<Game> findByHostIdOrderByCreatedAtDesc(UUID hostId, Limit limit);

    List<Game> findByStatus(GameStatus status);

    boolean existsByPinAndStatusNot(String pin, GameStatus status);

    boolean existsByPinAndCreatedAtAfter(String pin, Instant since);

    boolean existsByQuizIdAndStatusNot(UUID quizId, GameStatus status);
}

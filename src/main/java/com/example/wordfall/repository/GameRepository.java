package com.example.wordfall.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findByIdAndPlayerId(UUID id, UUID playerId);

    List<Game> findByPlayerIdAndStatus(UUID playerId, Game.Status status);

    Optional<Game> findTopByPlayerIdAndStatusOrderByScoreDesc(UUID playerId, Game.Status status);

    Page<Game> findByPlayerIdAndStatusOrderByFinishedAtDesc(UUID playerId, Game.Status status, Pageable pageable);
}

package com.example.wordfall.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.example.wordfall.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    /** 同じプレイヤーの書き込みを直列化するため、行ロックを取って読む。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.id = :id")
    Optional<Player> lockById(UUID id);
}

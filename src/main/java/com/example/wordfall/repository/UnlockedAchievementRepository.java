package com.example.wordfall.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.UnlockedAchievement;

public interface UnlockedAchievementRepository extends JpaRepository<UnlockedAchievement, Long> {

    boolean existsByPlayerIdAndAchievementKey(UUID playerId, String achievementKey);

    List<UnlockedAchievement> findByPlayerId(UUID playerId);
}

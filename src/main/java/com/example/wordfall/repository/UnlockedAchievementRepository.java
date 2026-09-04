package com.example.wordfall.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.UnlockedAchievement;

public interface UnlockedAchievementRepository extends JpaRepository<UnlockedAchievement,Long> {
    boolean existsByAchievementKey(String achievementKey);
}

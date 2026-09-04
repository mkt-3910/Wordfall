package com.example.wordfall.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class UnlockedAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String achievementKey;
    private LocalDateTime unlockedAt;

    public UnlockedAchievement() {
    }

    public UnlockedAchievement(String achievementKey, LocalDateTime unlockedAt) {
        this.achievementKey = achievementKey;
        this.unlockedAt = unlockedAt;
    }

    public Long getId() { return id; }
    public String getAchievementKey() { return achievementKey; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
}

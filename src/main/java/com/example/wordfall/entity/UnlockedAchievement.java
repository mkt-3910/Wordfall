package com.example.wordfall.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UnlockedAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false, length = 50)
    private String achievementKey;

    @Column(nullable = false)
    private LocalDateTime unlockedAt;

    protected UnlockedAchievement() {
    }

    public UnlockedAchievement(UUID playerId, String achievementKey, LocalDateTime unlockedAt) {
        this.playerId = playerId;
        this.achievementKey = achievementKey;
        this.unlockedAt = unlockedAt;
    }

    public Long getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public String getAchievementKey() { return achievementKey; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
}

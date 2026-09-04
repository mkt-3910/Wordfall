package com.example.wordfall.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.GameStats;

public interface GameStatsRepository extends JpaRepository<GameStats,Long> {
    
}

package com.example.wordfall.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wordfall.entity.GameStats;
import com.example.wordfall.entity.UnlockedAchievement;
import com.example.wordfall.model.AchievementDefinition;
import com.example.wordfall.repository.GameStatsRepository;
import com.example.wordfall.repository.UnlockedAchievementRepository;

@Service
public class AchievementService {

    private static final List<AchievementDefinition> DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
            new AchievementDefinition("word_10", "はじめの一歩", "累計10単語を完成させた"),
            new AchievementDefinition("word_50", "単語コレクター", "累計50単語を完成させた"),
            new AchievementDefinition("word_100", "単語マスター", "累計100単語を完成させた"),
            new AchievementDefinition("first_4letter", "一歩先へ", "はじめて4文字以上の単語を完成させた"),
            new AchievementDefinition("first_5letter", "語彙の探求者", "はじめて5文字以上の単語を完成させた"),
            new AchievementDefinition("combo_5", "波に乗る", "コンボを5まで伸ばした"),
            new AchievementDefinition("combo_10", "止まらない", "コンボを10まで伸ばした"),
            new AchievementDefinition("multi_2", "同時発見", "1回の着地で2単語同時に完成させた"),
            new AchievementDefinition("multi_3", "連鎖の使い手", "1回の着地で3単語以上同時に完成させた"),
            new AchievementDefinition("score_1000", "1000点の壁", "スコア1000点を達成した")
    ));

    private final GameStatsRepository gameStatsRepository;
    private final UnlockedAchievementRepository unlockedAchievementRepository;

    public AchievementService(GameStatsRepository gameStatsRepository,
                              UnlockedAchievementRepository unlockedAchievementRepository) {
        this.gameStatsRepository = gameStatsRepository;
        this.unlockedAchievementRepository = unlockedAchievementRepository;
    }

    public List<AchievementDefinition> getAllDefinitions() {
        return DEFINITIONS;
    }

    @Transactional
    public synchronized void checkAndUnlock(int wordsCompletedThisGame, int bestComboThisGame,
                               int bestMultiWordThisGame, boolean got4LetterThisGame,
                               boolean got5LetterThisGame, int finalScore) {
        GameStats stats = gameStatsRepository.findById(1L).orElseGet(GameStats::new);

        stats.setTotalWordsCompleted(Math.addExact(
                stats.getTotalWordsCompleted(), (long) wordsCompletedThisGame));
        stats.setBestCombo(Math.max(stats.getBestCombo(), bestComboThisGame));
        stats.setBestMultiWord(Math.max(stats.getBestMultiWord(), bestMultiWordThisGame));
        stats.setUnlockedFirst4Letter(stats.isUnlockedFirst4Letter() || got4LetterThisGame);
        stats.setUnlockedFirst5Letter(stats.isUnlockedFirst5Letter() || got5LetterThisGame);
        gameStatsRepository.save(stats);

        unlockWhen(stats.getTotalWordsCompleted() >= 10, "word_10");
        unlockWhen(stats.getTotalWordsCompleted() >= 50, "word_50");
        unlockWhen(stats.getTotalWordsCompleted() >= 100, "word_100");
        unlockWhen(stats.isUnlockedFirst4Letter(), "first_4letter");
        unlockWhen(stats.isUnlockedFirst5Letter(), "first_5letter");
        unlockWhen(stats.getBestCombo() >= 5, "combo_5");
        unlockWhen(stats.getBestCombo() >= 10, "combo_10");
        unlockWhen(stats.getBestMultiWord() >= 2, "multi_2");
        unlockWhen(stats.getBestMultiWord() >= 3, "multi_3");
        unlockWhen(finalScore >= 1000, "score_1000");
    }

    private void unlockWhen(boolean achieved, String key) {
        if (achieved && !unlockedAchievementRepository.existsByAchievementKey(key)) {
            unlockedAchievementRepository.save(new UnlockedAchievement(key, LocalDateTime.now()));
        }
    }
}

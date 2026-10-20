package com.example.wordfall.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wordfall.dto.ViewDtos;
import com.example.wordfall.entity.Player;
import com.example.wordfall.entity.UnlockedAchievement;
import com.example.wordfall.repository.UnlockedAchievementRepository;

@Service
public class AchievementService {

    /** 実績の定義と解除条件。条件は「プレイヤーの累計」と「今のプレイのスコア」から判定する。 */
    private record Definition(String key, String title, String description, Predicate<Progress> achieved) { }

    private record Progress(Player player, long gameScore) { }

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("word_10", "はじめの一歩", "累計10単語を完成させた", p -> p.player().getTotalWordsCompleted() >= 10),
            new Definition("word_50", "単語コレクター", "累計50単語を完成させた", p -> p.player().getTotalWordsCompleted() >= 50),
            new Definition("word_100", "単語マスター", "累計100単語を完成させた", p -> p.player().getTotalWordsCompleted() >= 100),
            new Definition("first_4letter", "一歩先へ", "はじめて4文字以上の単語を完成させた", p -> p.player().isCompletedFourLetter()),
            new Definition("first_5letter", "語彙の探求者", "はじめて5文字以上の単語を完成させた", p -> p.player().isCompletedFiveLetter()),
            new Definition("combo_5", "波に乗る", "コンボを5まで伸ばした", p -> p.player().getBestCombo() >= 5),
            new Definition("combo_10", "止まらない", "コンボを10まで伸ばした", p -> p.player().getBestCombo() >= 10),
            new Definition("multi_2", "同時発見", "1回の着地で2単語同時に完成させた", p -> p.player().getBestMultiWord() >= 2),
            new Definition("multi_3", "連鎖の使い手", "1回の着地で3単語以上同時に完成させた", p -> p.player().getBestMultiWord() >= 3),
            new Definition("score_1000", "1000点の壁", "スコア1000点を達成した", p -> p.gameScore() >= 1000));

    private final UnlockedAchievementRepository unlocked;

    public AchievementService(UnlockedAchievementRepository unlocked) {
        this.unlocked = unlocked;
    }

    @Transactional(readOnly = true)
    public List<ViewDtos.Achievement> getAchievements(UUID playerId) {
        Set<String> keys = unlocked.findByPlayerId(playerId).stream()
                .map(UnlockedAchievement::getAchievementKey)
                .collect(Collectors.toSet());
        return DEFINITIONS.stream()
                .map(d -> new ViewDtos.Achievement(d.key(), d.title(), d.description(), keys.contains(d.key())))
                .toList();
    }

    /** 条件を満たした未解除の実績を解除する。呼び出し元でプレイヤー行をロックしておくこと。 */
    @Transactional
    public void unlockReached(Player player, long gameScore) {
        Progress progress = new Progress(player, gameScore);
        for (Definition definition : DEFINITIONS) {
            if (definition.achieved().test(progress)
                    && !unlocked.existsByPlayerIdAndAchievementKey(player.getId(), definition.key())) {
                unlocked.save(new UnlockedAchievement(player.getId(), definition.key(), LocalDateTime.now()));
            }
        }
    }
}

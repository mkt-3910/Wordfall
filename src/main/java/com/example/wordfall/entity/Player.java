package com.example.wordfall.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** ブラウザ(Cookie)ごとのプレイヤー。実績判定に使う累計値もここに持つ。 */
@Entity
public class Player {

    @Id
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private long totalWordsCompleted;
    private int bestCombo;
    private int bestMultiWord;
    private boolean completedFourLetter;
    private boolean completedFiveLetter;

    protected Player() {
    }

    public Player(UUID id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    /** 1回の着地の結果を累計に反映する。 */
    public void recordLanding(int wordsCompleted, int combo, boolean fourLetter, boolean fiveLetter) {
        totalWordsCompleted = Math.addExact(totalWordsCompleted, wordsCompleted);
        bestCombo = Math.max(bestCombo, combo);
        bestMultiWord = Math.max(bestMultiWord, wordsCompleted);
        completedFourLetter |= fourLetter;
        completedFiveLetter |= fiveLetter;
    }

    public UUID getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getTotalWordsCompleted() { return totalWordsCompleted; }
    public int getBestCombo() { return bestCombo; }
    public int getBestMultiWord() { return bestMultiWord; }
    public boolean isCompletedFourLetter() { return completedFourLetter; }
    public boolean isCompletedFiveLetter() { return completedFiveLetter; }
}

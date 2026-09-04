package com.example.wordfall.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class GameStats {

    @Id
    private Long id = 1L; // 常に1行だけ使う固定ID

    private long totalWordsCompleted = 0;
    private int bestCombo = 0;
    private int bestMultiWord = 0;
    private boolean unlockedFirst4Letter = false;
    private boolean unlockedFirst5Letter = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getTotalWordsCompleted() {
        return totalWordsCompleted;
    }

    public void setTotalWordsCompleted(long totalWordsCompleted) {
        this.totalWordsCompleted = totalWordsCompleted;
    }

    public int getBestCombo() {
        return bestCombo;
    }

    public void setBestCombo(int bestCombo) {
        this.bestCombo = bestCombo;
    }

    public int getBestMultiWord() {
        return bestMultiWord;
    }

    public void setBestMultiWord(int bestMultiWord) {
        this.bestMultiWord = bestMultiWord;
    }

    public boolean isUnlockedFirst4Letter() {
        return unlockedFirst4Letter;
    }

    public void setUnlockedFirst4Letter(boolean unlockedFirst4Letter) {
        this.unlockedFirst4Letter = unlockedFirst4Letter;
    }

    public boolean isUnlockedFirst5Letter() {
        return unlockedFirst5Letter;
    }

    public void setUnlockedFirst5Letter(boolean unlockedFirst5Letter) {
        this.unlockedFirst5Letter = unlockedFirst5Letter;
    }
}

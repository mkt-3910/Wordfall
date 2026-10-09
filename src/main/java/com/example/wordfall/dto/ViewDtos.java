package com.example.wordfall.dto;

import java.time.LocalDateTime;

/** 履歴・単語帳・実績の画面向けレスポンス。 */
public final class ViewDtos {

    private ViewDtos() {
    }

    public record HighScore(long score) { }

    public record ScoreEntry(long score, int wordCount, String words, LocalDateTime createdAt) { }

    public record WordEntry(String word, String partOfSpeech, String meaning) { }

    public record Achievement(String key, String title, String description, boolean unlocked) { }
}

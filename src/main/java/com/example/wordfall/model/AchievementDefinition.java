package com.example.wordfall.model;

/** 実績の表示情報と判定キーを表す不変オブジェクト。 */
public class AchievementDefinition {

    private final String key;
    private final String title;
    private final String description;

    public AchievementDefinition(String key, String title, String description) {
        this.key = key;
        this.title = title;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

package com.example.wordfall.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class WordLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false, length = 32)
    private String word;

    @Column(nullable = false, length = 50)
    private String partOfSpeech;

    @Column(nullable = false, length = 1000)
    private String meaning;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected WordLog() {
    }

    public WordLog(UUID playerId, String word, String partOfSpeech, String meaning, LocalDateTime createdAt) {
        this.playerId = playerId;
        this.word = word;
        this.partOfSpeech = partOfSpeech;
        this.meaning = meaning;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public String getWord() { return word; }
    public String getPartOfSpeech() { return partOfSpeech; }
    public String getMeaning() { return meaning; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 辞書の更新を単語帳にも反映する。 */
    public void updateMeaning(String partOfSpeech, String meaning) {
        this.partOfSpeech = partOfSpeech;
        this.meaning = meaning;
    }
}

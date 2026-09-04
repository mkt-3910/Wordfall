package com.example.wordfall.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class WordLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String word;
    @Column(length = 50)
    private String partOfSpeech;

    @Column(nullable = false, length = 1000)
    private String meaning;
    private LocalDateTime createdAt;

    public WordLog() {
    }

    public WordLog(String word, String partOfSpeech, String meaning, LocalDateTime createdAt) {
        this.word = word;
        this.partOfSpeech = partOfSpeech;
        this.meaning = meaning;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public String getMeaning() {
        return meaning;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

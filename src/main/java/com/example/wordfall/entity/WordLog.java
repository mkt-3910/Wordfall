package com.example.wordfall.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class WordLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String word;
    private String partOfSpeech;
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

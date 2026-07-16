package com.example.wordfall.controller;

// JSでPOSTするときの「受け取り用」の入れ物。
// Scoreエンティティと分けているのは、idやplayedAtまで外部から自由に指定されると困るため。
public class ScoreRequest {

    private int score;
    private int wordCount;
    private String words;

    public ScoreRequest() {

    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }
}

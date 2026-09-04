package com.example.wordfall.controller;

public class AchievementCheckRequest {

    private int wordsCompletedThisGame;
    private int bestComboThisGame;
    private int bestMultiWordThisGame;
    private boolean got4LetterThisGame;
    private boolean got5LetterThisGame;
    private int finalScore;

    public int getWordsCompletedThisGame() {
        return wordsCompletedThisGame;
    }

    public void setWordsCompletedThisGame(int wordsCompletedThisGame) {
        this.wordsCompletedThisGame = wordsCompletedThisGame;
    }

    public int getBestComboThisGame() {
        return bestComboThisGame;
    }

    public void setBestComboThisGame(int bestComboThisGame) {
        this.bestComboThisGame = bestComboThisGame;
    }

    public int getBestMultiWordThisGame() {
        return bestMultiWordThisGame;
    }

    public void setBestMultiWordThisGame(int bestMultiWordThisGame) {
        this.bestMultiWordThisGame = bestMultiWordThisGame;
    }

    public boolean isGot4LetterThisGame() {
        return got4LetterThisGame;
    }

    public void setGot4LetterThisGame(boolean got4LetterThisGame) {
        this.got4LetterThisGame = got4LetterThisGame;
    }

    public boolean isGot5LetterThisGame() {
        return got5LetterThisGame;
    }

    public void setGot5LetterThisGame(boolean got5LetterThisGame) {
        this.got5LetterThisGame = got5LetterThisGame;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }
}

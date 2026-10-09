package com.example.wordfall.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import com.example.wordfall.game.Board;
import com.example.wordfall.game.GameEngine.EndReason;
import com.example.wordfall.game.GameState;
import com.example.wordfall.game.Piece;
import com.example.wordfall.game.Rng;
import com.example.wordfall.game.Shape;

/** 1プレイ分の記録。プレイ中は盤面などの途中状態を持ち、終了後はプレイ履歴として表示する。 */
@Entity
public class Game {

    public enum Status { IN_PROGRESS, FINISHED, ABANDONED }

    private static final int MAX_TEXT_LENGTH = 1_000_000;

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    private int rngState;

    @Column(nullable = false, length = Board.ROWS * Board.COLS)
    private String board;

    @Column(nullable = false, length = 1)
    private String pieceShape;

    @Column(nullable = false, length = 4)
    private String pieceLetters;

    private long score;
    private int life;
    private int combo;
    private int landingCount;
    private int wordCount;

    @Column(nullable = false, length = MAX_TEXT_LENGTH)
    private String words;

    @Column(length = 64)
    private String lastLandingKey;

    @Column(length = MAX_TEXT_LENGTH)
    private String lastLandingResponse;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private EndReason endReason;

    private long highScore;
    private boolean newHighScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    protected Game() {
    }

    public Game(UUID id, UUID playerId, GameState state, LocalDateTime createdAt) {
        this.id = id;
        this.playerId = playerId;
        this.status = Status.IN_PROGRESS;
        this.words = "";
        this.createdAt = createdAt;
        copyState(state);
    }

    public GameState toState() {
        Piece piece = new Piece(Shape.valueOf(pieceShape), pieceLetters);
        return new GameState(Board.parse(board), new Rng(rngState), piece, score, life, combo, landingCount, wordCount);
    }

    /** GameEngine で進めた状態と、新しく完成した単語を記録する。 */
    public void applyState(GameState state, List<String> completedWords) {
        copyState(state);
        if (!completedWords.isEmpty()) {
            String added = String.join(",", completedWords);
            this.words = words.isEmpty() ? added : words + "," + added;
        }
    }

    private void copyState(GameState state) {
        this.rngState = state.rng().state();
        this.board = state.board().serialize();
        this.pieceShape = state.piece().shape().name();
        this.pieceLetters = state.piece().letters();
        this.score = state.score();
        this.life = state.life();
        this.combo = state.combo();
        this.landingCount = state.landingCount();
        this.wordCount = state.wordCount();
    }

    /** 同じ着地リクエストの再送に同じ応答を返せるよう、直前の着地を覚えておく。 */
    public void rememberLanding(String key, String responseJson) {
        this.lastLandingKey = key;
        this.lastLandingResponse = responseJson;
    }

    /** 着地が1回もないプレイは履歴に残さない。 */
    public void finish(EndReason reason, long previousHighScore, LocalDateTime now) {
        this.status = landingCount == 0 ? Status.ABANDONED : Status.FINISHED;
        this.endReason = reason;
        this.highScore = Math.max(previousHighScore, score);
        this.newHighScore = score > previousHighScore;
        this.finishedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public Status getStatus() { return status; }
    public long getScore() { return score; }
    public int getLandingCount() { return landingCount; }
    public int getWordCount() { return wordCount; }
    public String getWords() { return words; }
    public String getLastLandingKey() { return lastLandingKey; }
    public String getLastLandingResponse() { return lastLandingResponse; }
    public EndReason getEndReason() { return endReason; }
    public long getHighScore() { return highScore; }
    public boolean isNewHighScore() { return newHighScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}

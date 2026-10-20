package com.example.wordfall.game;

/** 1プレイの途中状態。GameEngine だけが更新し、保存・復元は Game エンティティが行う。 */
public final class GameState {

    private final Board board;
    private final Rng rng;
    private Piece piece;
    private long score;
    private int life;
    private int combo;
    private int landingCount;
    private int wordCount;

    public GameState(Board board, Rng rng, Piece piece, long score, int life, int combo,
                     int landingCount, int wordCount) {
        this.board = board;
        this.rng = rng;
        this.piece = piece;
        this.score = score;
        this.life = life;
        this.combo = combo;
        this.landingCount = landingCount;
        this.wordCount = wordCount;
    }

    public Board board() { return board; }
    public Rng rng() { return rng; }
    public Piece piece() { return piece; }
    public long score() { return score; }
    public int life() { return life; }
    public int combo() { return combo; }
    public int landingCount() { return landingCount; }
    public int wordCount() { return wordCount; }

    void setPiece(Piece piece) { this.piece = piece; }
    void addScore(long points) { this.score = Math.addExact(score, points); }
    void loseLife() { this.life--; }
    void setCombo(int combo) { this.combo = combo; }
    void countLanding() { this.landingCount++; }
    void countWord() { this.wordCount++; }
}

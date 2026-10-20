package com.example.wordfall.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 12列×13行の盤面。空きマスは '.' で表し、DB保存と画面への送信に同じ文字列表現を使う。 */
public final class Board {

    public static final int COLS = 12;
    public static final int ROWS = 13;
    static final char EMPTY = '.';

    private final char[][] cells = new char[ROWS][COLS];

    public Board() {
        clear();
    }

    public static Board parse(String text) {
        if (text == null || text.length() != ROWS * COLS) throw new IllegalArgumentException("Invalid board");
        Board board = new Board();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                char ch = text.charAt(r * COLS + c);
                if (ch != EMPTY && (ch < 'A' || ch > 'Z')) throw new IllegalArgumentException("Invalid board");
                board.cells[r][c] = ch;
            }
        }
        return board;
    }

    public String serialize() {
        StringBuilder text = new StringBuilder(ROWS * COLS);
        for (char[] row : cells) text.append(row);
        return text.toString();
    }

    /** 画面に渡す行ごとの表現。 */
    public List<String> rows() {
        List<String> rows = new ArrayList<>(ROWS);
        for (char[] row : cells) rows.add(new String(row));
        return rows;
    }

    public char get(int r, int c) {
        return cells[r][c];
    }

    public boolean isEmpty(int r, int c) {
        return cells[r][c] == EMPTY;
    }

    void set(int r, int c, char ch) {
        cells[r][c] = ch;
    }

    void clear() {
        for (char[] row : cells) Arrays.fill(row, EMPTY);
    }

    /** 画面側の collides と同じ判定。上方向(y<0)ははみ出しても衝突扱いにしない。 */
    public boolean collides(List<Piece.Cell> piece, int x, int y) {
        for (Piece.Cell cell : piece) {
            int gx = x + cell.x();
            int gy = y + cell.y();
            if (gx < 0 || gx >= COLS || gy >= ROWS) return true;
            if (gy >= 0 && cells[gy][gx] != EMPTY) return true;
        }
        return false;
    }

    void place(List<Piece.Cell> piece, int x, int y) {
        for (Piece.Cell cell : piece) {
            int gy = y + cell.y();
            if (gy >= 0) cells[gy][x + cell.x()] = cell.letter();
        }
    }

    /** 文字を列ごとに下へ詰める。 */
    void applyGravity() {
        for (int c = 0; c < COLS; c++) {
            int writeRow = ROWS - 1;
            for (int r = ROWS - 1; r >= 0; r--) {
                if (cells[r][c] != EMPTY) {
                    char ch = cells[r][c];
                    cells[r][c] = EMPTY;
                    cells[writeRow--][c] = ch;
                }
            }
        }
    }
}

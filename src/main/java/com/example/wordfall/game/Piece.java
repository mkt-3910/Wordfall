package com.example.wordfall.game;

import java.util.ArrayList;
import java.util.List;

/** 回転前の向きのミノ。letters は Shape の cells と同じ順番の文字。 */
public record Piece(Shape shape, String letters) {

    public Piece {
        if (shape == null || letters == null || letters.length() != shape.cells().length) {
            throw new IllegalArgumentException("Invalid piece");
        }
    }

    /** 出現位置(x)。y は常に0。 */
    public int spawnX() {
        return (Board.COLS - shape.size()) / 2;
    }

    public List<Cell> cells() {
        return rotated(0);
    }

    /** 時計回りに times 回まわした各マス。画面側の回転 (x, y) → (size-1-y, x) と同じ。 */
    public List<Cell> rotated(int times) {
        List<Cell> result = new ArrayList<>();
        int[][] base = shape.cells();
        for (int i = 0; i < base.length; i++) {
            int x = base[i][0];
            int y = base[i][1];
            for (int r = 0; r < times; r++) {
                int nextX = shape.size() - 1 - y;
                y = x;
                x = nextX;
            }
            result.add(new Cell(x, y, letters.charAt(i)));
        }
        return result;
    }

    public record Cell(int x, int y, char letter) { }
}

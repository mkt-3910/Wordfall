package com.example.wordfall.game;

import java.util.List;

/** ミノの形。cells は回転前の座標 [x, y]、wordLines は単語を仕込める一直線の並び。 */
public enum Shape {
    I(4, new int[][] {{0, 1}, {1, 1}, {2, 1}, {3, 1}},
            List.of(new int[][] {{0, 1}, {1, 1}, {2, 1}}, new int[][] {{1, 1}, {2, 1}, {3, 1}})),
    O(4, new int[][] {{1, 0}, {2, 0}, {1, 1}, {2, 1}}, List.of()),
    T(3, new int[][] {{1, 0}, {0, 1}, {1, 1}, {2, 1}}, List.<int[][]>of(new int[][] {{0, 1}, {1, 1}, {2, 1}})),
    S(3, new int[][] {{1, 0}, {2, 0}, {0, 1}, {1, 1}}, List.of()),
    Z(3, new int[][] {{0, 0}, {1, 0}, {1, 1}, {2, 1}}, List.of()),
    J(3, new int[][] {{0, 0}, {0, 1}, {1, 1}, {2, 1}}, List.<int[][]>of(new int[][] {{0, 1}, {1, 1}, {2, 1}})),
    L(3, new int[][] {{2, 0}, {0, 1}, {1, 1}, {2, 1}}, List.<int[][]>of(new int[][] {{0, 1}, {1, 1}, {2, 1}}));

    /** I ミノの横一列に4文字単語を仕込むときの並び。 */
    static final int[][] FOUR_LETTER_LINE = {{0, 1}, {1, 1}, {2, 1}, {3, 1}};

    private final int size;
    private final int[][] cells;
    private final List<int[][]> wordLines;

    Shape(int size, int[][] cells, List<int[][]> wordLines) {
        this.size = size;
        this.cells = cells;
        this.wordLines = wordLines;
    }

    public int size() {
        return size;
    }

    int[][] cells() {
        return cells;
    }

    List<int[][]> wordLines() {
        return wordLines;
    }
}

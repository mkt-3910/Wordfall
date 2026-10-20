package com.example.wordfall.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** 盤面やミノの中から、辞書にある英単語を探す。 */
public final class WordFinder {

    private static final int MIN_WORD_LENGTH = 3;
    private static final List<int[][]> LINES = allLines();

    private final Predicate<String> dictionary;

    public WordFinder(Predicate<String> dictionary) {
        this.dictionary = dictionary;
    }

    /** 縦・横・斜めの各列で、文字が3つ以上連続する部分から辞書の単語を長い順・重ならないように選ぶ。 */
    public List<Match> find(Board board) {
        List<Match> matches = new ArrayList<>();
        for (int[][] line : LINES) {
            for (List<int[]> run : runs(board, line)) {
                matches.addAll(wordsInRun(board, run));
            }
        }
        return matches;
    }

    /** ミノ1個だけで単語ができているか(=ラッキーミノか)。回転しても並びは変わらないので、回転前の向きで調べる。 */
    public boolean containsWord(Piece piece) {
        Board board = new Board();
        board.place(piece.cells(), 0, 0);
        return !find(board).isEmpty();
    }

    private static List<List<int[]>> runs(Board board, int[][] line) {
        List<List<int[]>> runs = new ArrayList<>();
        List<int[]> current = new ArrayList<>();
        for (int[] cell : line) {
            if (!board.isEmpty(cell[0], cell[1])) {
                current.add(cell);
            } else {
                if (current.size() >= MIN_WORD_LENGTH) runs.add(current);
                current = new ArrayList<>();
            }
        }
        if (current.size() >= MIN_WORD_LENGTH) runs.add(current);
        return runs;
    }

    private List<Match> wordsInRun(Board board, List<int[]> run) {
        StringBuilder text = new StringBuilder();
        for (int[] cell : run) text.append(board.get(cell[0], cell[1]));

        record Candidate(int start, int end, String word) { }
        List<Candidate> candidates = new ArrayList<>();
        for (int start = 0; start < text.length(); start++) {
            for (int end = start + MIN_WORD_LENGTH; end <= text.length(); end++) {
                String forward = text.substring(start, end);
                String backward = new StringBuilder(forward).reverse().toString();
                if (dictionary.test(forward)) candidates.add(new Candidate(start, end, forward));
                if (!backward.equals(forward) && dictionary.test(backward)) candidates.add(new Candidate(start, end, backward));
            }
        }
        candidates.sort(Comparator.comparingInt((Candidate c) -> c.end() - c.start()).reversed());

        boolean[] used = new boolean[run.size()];
        List<Match> matches = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean overlap = false;
            for (int i = candidate.start(); i < candidate.end(); i++) overlap |= used[i];
            if (overlap) continue;
            Arrays.fill(used, candidate.start(), candidate.end(), true);
            matches.add(new Match(candidate.word(), List.copyOf(run.subList(candidate.start(), candidate.end()))));
        }
        return matches;
    }

    private static List<int[][]> allLines() {
        List<int[][]> lines = new ArrayList<>();
        for (int r = 0; r < Board.ROWS; r++) lines.add(line(r, 0, 0, 1));
        for (int c = 0; c < Board.COLS; c++) lines.add(line(0, c, 1, 0));
        for (int c = 0; c < Board.COLS; c++) lines.add(line(0, c, 1, 1));
        for (int r = 1; r < Board.ROWS; r++) lines.add(line(r, 0, 1, 1));
        for (int c = 0; c < Board.COLS; c++) lines.add(line(Board.ROWS - 1, c, -1, 1));
        for (int r = Board.ROWS - 2; r >= 0; r--) lines.add(line(r, 0, -1, 1));
        return List.copyOf(lines);
    }

    private static int[][] line(int r, int c, int dr, int dc) {
        List<int[]> cells = new ArrayList<>();
        while (r >= 0 && r < Board.ROWS && c >= 0 && c < Board.COLS) {
            cells.add(new int[] {r, c});
            r += dr;
            c += dc;
        }
        return cells.toArray(int[][]::new);
    }

    /** 見つかった単語と、その [行, 列] の並び。 */
    public record Match(String word, List<int[]> cells) { }
}

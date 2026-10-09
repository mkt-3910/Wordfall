package com.example.wordfall.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/** ゲームのルール本体。クライアントから届くのはミノの着地位置だけで、単語・得点・次のミノはここで決める。 */
public final class GameEngine {

    public static final int START_LIFE = 3;
    public static final int MAX_LANDINGS = 10_000;
    private static final int MIN_WORD_LENGTH = 3;
    private static final List<int[][]> LINES = allLines();

    private final Predicate<String> dictionary;

    public GameEngine(Predicate<String> dictionary) {
        this.dictionary = dictionary;
    }

    /** 現在のミノを (x, y, rotation) に着地させ、単語の判定・消去・得点・次のミノの用意まで行う。 */
    public LandingOutcome land(GameState state, int x, int y, int rotation) {
        if (rotation < 0 || rotation > 3 || !isReachableLanding(state.board(), state.piece(), x, y, rotation)) {
            throw new IllegalMoveException();
        }
        Board board = state.board();
        board.place(state.piece().rotated(rotation), x, y);
        board.applyGravity();

        List<Match> matches = findWords(board);
        state.setCombo(matches.isEmpty() ? 0 : state.combo() + 1);
        int comboBonus = Math.max(0, state.combo() - 1) * 5;
        int multiWordBonus = matches.size() >= 2 ? matches.size() * 20 : 0;
        state.addScore(multiWordBonus);

        List<ScoredWord> words = new ArrayList<>();
        for (Match match : matches) {
            int points = match.word().length() * 10 + comboBonus;
            state.addScore(points);
            state.countWord();
            words.add(new ScoredWord(match.word(), points, match.cells()));
            for (int[] cell : match.cells()) board.set(cell[0], cell[1], Board.EMPTY);
        }
        board.applyGravity();

        state.countLanding();
        if (state.landingCount() >= MAX_LANDINGS) {
            return new LandingOutcome(words, multiWordBonus, false, EndReason.LANDING_LIMIT);
        }
        Piece next = PieceGenerator.next(state.rng());
        state.setPiece(next);
        if (board.collides(next.cells(), next.spawnX(), 0)) {
            state.loseLife();
            if (state.life() <= 0) return new LandingOutcome(words, multiWordBonus, true, EndReason.GAME_OVER);
            board.clear();
            return new LandingOutcome(words, multiWordBonus, true, null);
        }
        return new LandingOutcome(words, multiWordBonus, false, null);
    }

    /** 出現位置から左右移動・落下・回転だけでたどり着け、かつそれ以上落ちない位置かを調べる。 */
    static boolean isReachableLanding(Board board, Piece piece, int targetX, int targetY, int targetRotation) {
        List<List<Piece.Cell>> rotations = List.of(piece.rotated(0), piece.rotated(1), piece.rotated(2), piece.rotated(3));
        int startX = piece.spawnX();
        if (board.collides(rotations.get(0), startX, 0)) return false;

        Set<Integer> visited = new HashSet<>();
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {startX, 0, 0});
        visited.add(key(startX, 0, 0));
        while (!queue.isEmpty()) {
            int[] s = queue.poll();
            if (s[0] == targetX && s[1] == targetY && s[2] == targetRotation) {
                return board.collides(rotations.get(s[2]), s[0], s[1] + 1);
            }
            int[][] nextStates = {
                    {s[0] - 1, s[1], s[2]}, {s[0] + 1, s[1], s[2]}, {s[0], s[1] + 1, s[2]}, {s[0], s[1], (s[2] + 1) % 4}
            };
            for (int[] n : nextStates) {
                if (!board.collides(rotations.get(n[2]), n[0], n[1]) && visited.add(key(n[0], n[1], n[2]))) {
                    queue.add(n);
                }
            }
        }
        return false;
    }

    private static int key(int x, int y, int rotation) {
        return ((rotation * 64) + (y + 8)) * 64 + (x + 8);
    }

    /** 縦・横・斜めの各列で、文字が3つ以上連続する部分から辞書の単語を長い順・重ならないように選ぶ。 */
    List<Match> findWords(Board board) {
        List<Match> matches = new ArrayList<>();
        for (int[][] line : LINES) {
            for (List<int[]> run : runs(board, line)) {
                matches.addAll(wordsInRun(board, run));
            }
        }
        return matches;
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

    record Match(String word, List<int[]> cells) { }

    /** 完成した単語。cells は1回目の落下後の盤面での [行, 列]。 */
    public record ScoredWord(String word, int points, List<int[]> cells) { }

    /** endReason が null ならゲームは続く。 */
    public record LandingOutcome(List<ScoredWord> words, int multiWordBonus, boolean lifeLost, EndReason endReason) { }

    public enum EndReason { GAME_OVER, LANDING_LIMIT, QUIT }

    public static final class IllegalMoveException extends RuntimeException {
        public IllegalMoveException() {
            super("Illegal landing position");
        }
    }
}

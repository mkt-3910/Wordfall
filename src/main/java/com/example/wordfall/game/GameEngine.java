package com.example.wordfall.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

import com.example.wordfall.game.WordFinder.Match;

/** ゲームのルール本体。クライアントから届くのはミノの着地位置だけで、単語・得点・次のミノはここで決める。 */
public final class GameEngine {

    public static final int START_LIFE = 3;
    public static final int MAX_LANDINGS = 10_000;

    private final WordFinder words;
    private final PieceGenerator pieces;

    public GameEngine(Predicate<String> dictionary) {
        this.words = new WordFinder(dictionary);
        this.pieces = new PieceGenerator(words);
    }

    /** 新しいプレイの最初の状態を作る。 */
    public GameState newGame(int seed) {
        Rng rng = new Rng(seed);
        return new GameState(new Board(), rng, pieces.next(rng), 0, START_LIFE, 0, 0, 0);
    }

    /** ミノ1個だけで単語ができている「ラッキーミノ」か。 */
    public boolean isLucky(Piece piece) {
        return words.containsWord(piece);
    }

    /** 現在のミノを (x, y, rotation) に着地させ、単語の判定・消去・得点・次のミノの用意まで行う。 */
    public LandingOutcome land(GameState state, int x, int y, int rotation) {
        if (rotation < 0 || rotation > 3 || !isReachableLanding(state.board(), state.piece(), x, y, rotation)) {
            throw new IllegalMoveException();
        }
        Board board = state.board();
        board.place(state.piece().rotated(rotation), x, y);
        board.applyGravity();

        List<Match> matches = words.find(board);
        state.setCombo(matches.isEmpty() ? 0 : state.combo() + 1);
        int comboBonus = Math.max(0, state.combo() - 1) * 5;
        int multiWordBonus = matches.size() >= 2 ? matches.size() * 20 : 0;
        state.addScore(multiWordBonus);

        List<ScoredWord> completed = new ArrayList<>();
        for (Match match : matches) {
            int points = match.word().length() * 10 + comboBonus;
            state.addScore(points);
            state.countWord();
            completed.add(new ScoredWord(match.word(), points, match.cells()));
            for (int[] cell : match.cells()) board.set(cell[0], cell[1], Board.EMPTY);
        }
        board.applyGravity();

        state.countLanding();
        if (state.landingCount() >= MAX_LANDINGS) {
            return new LandingOutcome(completed, multiWordBonus, false, EndReason.LANDING_LIMIT);
        }
        Piece next = pieces.next(state.rng());
        state.setPiece(next);
        if (board.collides(next.cells(), next.spawnX(), 0)) {
            state.loseLife();
            if (state.life() <= 0) return new LandingOutcome(completed, multiWordBonus, true, EndReason.GAME_OVER);
            board.clear();
            return new LandingOutcome(completed, multiWordBonus, true, null);
        }
        return new LandingOutcome(completed, multiWordBonus, false, null);
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

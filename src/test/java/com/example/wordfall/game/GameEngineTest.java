package com.example.wordfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.wordfall.game.GameEngine.EndReason;
import com.example.wordfall.game.GameEngine.IllegalMoveException;
import com.example.wordfall.game.GameEngine.LandingOutcome;

class GameEngineTest {

    private final GameEngine engine = new GameEngine(Set.of("CAT", "DOG", "BOOK")::contains);
    private final WordFinder finder = new WordFinder(Set.of("CAT", "DOG", "BOOK")::contains);

    /** rows は上から順の行。指定しない上の行は空きマスにする。 */
    private static Board board(String... bottomRows) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < Board.ROWS - bottomRows.length; i++) text.append(".".repeat(Board.COLS));
        for (String row : bottomRows) text.append(row);
        return Board.parse(text.toString());
    }

    private static GameState state(Board board, Piece piece, int combo, int life) {
        return new GameState(board, new Rng(42), piece, 0, life, combo, 0, 0);
    }

    @Test
    void completesHorizontalWordAndScoresItOnTheServer() {
        GameState state = state(board("CA.........."), new Piece(Shape.I, "TQQQ"), 0, 3);

        LandingOutcome outcome = engine.land(state, 2, 11, 0);

        assertEquals(List.of("CAT"), outcome.words().stream().map(GameEngine.ScoredWord::word).toList());
        assertEquals(30, state.score());
        assertEquals(1, state.combo());
        assertEquals(1, state.wordCount());
        assertEquals(1, state.landingCount());
        assertEquals("...QQQ......", state.board().rows().get(Board.ROWS - 1));
        assertNull(outcome.endReason());
    }

    @Test
    void sharedLetterCompletesTwoWordsWithComboAndMultiWordBonus() {
        // 縦に置いた I ミノの T が、横の CA と縦の CA の両方を完成させる
        GameState state = state(board("CA.........."), new Piece(Shape.I, "QCAT"), 1, 3);

        LandingOutcome outcome = engine.land(state, 0, 9, 1);

        assertEquals(2, outcome.words().size());
        assertEquals(40, outcome.multiWordBonus());
        // (30 + コンボ2のボーナス5) × 2語 + 同時ボーナス40
        assertEquals(110, state.score());
        assertEquals(2, state.combo());
    }

    @Test
    void readsWordsBackwardsAndDiagonally() {
        Board board = board(
                "..T.........",
                ".QA.........",
                "QQC.........");
        // 縦(下から上へ)の CAT と、斜めの組み合わせを確認する
        List<WordFinder.Match> matches = finder.find(board);
        assertEquals(List.of("CAT"), matches.stream().map(WordFinder.Match::word).toList());

        Board diagonal = board(
                "..T.........",
                ".AQ.........",
                "CQQ.........");
        assertEquals(List.of("CAT"), finder.find(diagonal).stream().map(WordFinder.Match::word).toList());
    }

    @Test
    void comboResetsWhenNoWordIsCompleted() {
        GameState state = state(board(), new Piece(Shape.O, "QQQQ"), 4, 3);
        engine.land(state, 4, 11, 0);
        assertEquals(0, state.combo());
        assertEquals(0, state.score());
    }

    @Test
    void rejectsFloatingUnreachableAndMalformedLandings() {
        GameState state = state(board(), new Piece(Shape.O, "QQQQ"), 0, 3);
        assertThrows(IllegalMoveException.class, () -> engine.land(state, 4, 5, 0));
        assertThrows(IllegalMoveException.class, () -> engine.land(state, 40, 11, 0));
        assertThrows(IllegalMoveException.class, () -> engine.land(state, 4, 11, 4));
        assertThrows(IllegalMoveException.class, () -> engine.land(state, 4, 11, -1));
        assertEquals(0, state.landingCount());
        assertEquals(".".repeat(Board.ROWS * Board.COLS), state.board().serialize());
    }

    @Test
    void reachabilityFollowsTheSameMovesAsTheBrowser() {
        Board board = board();
        Piece piece = new Piece(Shape.I, "QQQQ");
        assertTrue(GameEngine.isReachableLanding(board, piece, 0, 9, 1));
        assertTrue(GameEngine.isReachableLanding(board, piece, -2, 9, 1));
        assertFalse(GameEngine.isReachableLanding(board, piece, -3, 9, 1));
        assertFalse(GameEngine.isReachableLanding(board, piece, 0, 8, 1));
    }

    @Test
    void blockedSpawnCostsALifeAndClearsTheBoard() {
        GameState state = state(blockedCenter(), new Piece(Shape.O, "QQQQ"), 0, 3);

        LandingOutcome outcome = engine.land(state, 4, 0, 0);

        assertTrue(outcome.lifeLost());
        assertNull(outcome.endReason());
        assertEquals(2, state.life());
        assertEquals(".".repeat(Board.ROWS * Board.COLS), state.board().serialize());
    }

    @Test
    void lastLifeEndsTheGame() {
        GameState state = state(blockedCenter(), new Piece(Shape.O, "QQQQ"), 0, 1);
        assertEquals(EndReason.GAME_OVER, engine.land(state, 4, 0, 0).endReason());
        assertEquals(0, state.life());
    }

    @Test
    void boardTextRoundTripsAndRejectsInvalidCharacters() {
        Board board = board("CAT.........");
        assertEquals(board.serialize(), Board.parse(board.serialize()).serialize());
        assertThrows(IllegalArgumentException.class, () -> Board.parse("x".repeat(Board.ROWS * Board.COLS)));
        assertThrows(IllegalArgumentException.class, () -> Board.parse("."));
    }

    /** 中央の列(3〜8)を上から3行目まで埋め、出現位置のすぐ下までふさいだ盤面。 */
    private static Board blockedCenter() {
        String[] rows = new String[Board.ROWS - 2];
        for (int i = 0; i < rows.length; i++) rows[i] = "...QQQQQQ...";
        return board(rows);
    }
}

package com.example.wordfall.dto;

import java.util.List;
import java.util.UUID;

import com.example.wordfall.game.Piece;

/** ゲーム進行APIの入出力。 */
public final class GameDtos {

    private GameDtos() {
    }

    public record StartResponse(UUID gameId, int maxLandings, int life, List<String> board, PieceView piece) { }

    /** index はこれまでの着地回数。再送を見分けるために使う。 */
    public record LandingRequest(Integer index, Integer x, Integer y, Integer rotation) { }

    /** nextPiece と result はどちらか一方だけが入る(result があればゲーム終了)。 */
    public record LandingResponse(int index, List<WordResult> words, int multiWordBonus, int combo, long score,
                                  int wordCount, int life, boolean lifeLost, List<String> board,
                                  PieceView nextPiece, GameResult result) { }

    /** cells は1回目の落下後の盤面での [行, 列]。 */
    public record WordResult(String word, String partOfSpeech, String definition, int points, List<int[]> cells) { }

    public record GameResult(String reason, long score, int wordCount, long highScore, boolean newHighScore) { }

    /** 回転前の向きのミノ。x, y は出現位置。 */
    public record PieceView(int size, int x, int y, List<CellView> cells) {
        public static PieceView of(Piece piece) {
            List<CellView> cells = piece.cells().stream()
                    .map(cell -> new CellView(cell.x(), cell.y(), String.valueOf(cell.letter())))
                    .toList();
            return new PieceView(piece.shape().size(), piece.spawnX(), 0, cells);
        }
    }

    public record CellView(int x, int y, String letter) { }
}

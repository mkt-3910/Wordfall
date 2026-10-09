package com.example.wordfall.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wordfall.dto.GameDtos.GameResult;
import com.example.wordfall.dto.GameDtos.LandingRequest;
import com.example.wordfall.dto.GameDtos.LandingResponse;
import com.example.wordfall.dto.GameDtos.PieceView;
import com.example.wordfall.dto.GameDtos.StartResponse;
import com.example.wordfall.dto.GameDtos.WordResult;
import com.example.wordfall.entity.Game;
import com.example.wordfall.entity.Player;
import com.example.wordfall.exception.BadRequestException;
import com.example.wordfall.exception.ConflictException;
import com.example.wordfall.exception.NotFoundException;
import com.example.wordfall.game.GameEngine;
import com.example.wordfall.game.GameEngine.EndReason;
import com.example.wordfall.game.GameEngine.LandingOutcome;
import com.example.wordfall.game.GameEngine.ScoredWord;
import com.example.wordfall.game.GameState;
import com.example.wordfall.game.Piece;
import com.example.wordfall.model.MeaningResponse;
import com.example.wordfall.repository.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ゲームの進行を管理する。盤面・単語判定・得点はすべてサーバーで計算し、
 * クライアントから受け取るのはミノの着地位置だけにする(スコアの改ざん対策)。
 */
@Service
public class GameService {

    private final GameRepository games;
    private final PlayerService players;
    private final AchievementService achievements;
    private final WordLogService wordLog;
    private final DictionaryService dictionary;
    private final GameEngine engine;
    private final ObjectMapper json;
    private final SecureRandom random = new SecureRandom();

    public GameService(GameRepository games, PlayerService players, AchievementService achievements,
                       WordLogService wordLog, DictionaryService dictionary, ObjectMapper json) {
        this.games = games;
        this.players = players;
        this.achievements = achievements;
        this.wordLog = wordLog;
        this.dictionary = dictionary;
        this.engine = new GameEngine(dictionary::hasGuaranteedMeaning);
        this.json = json;
    }

    /** 新しいプレイを始める。途中のプレイが残っていれば「中断」として終了させる。 */
    @Transactional
    public StartResponse start(UUID playerId) {
        players.ensureExists(playerId);
        players.lock(playerId);
        for (Game unfinished : games.findByPlayerIdAndStatus(playerId, Game.Status.IN_PROGRESS)) {
            finish(unfinished, EndReason.QUIT);
        }
        GameState state = engine.newGame(random.nextInt());
        Game game = games.save(new Game(UUID.randomUUID(), playerId, state, LocalDateTime.now()));
        return new StartResponse(game.getId(), GameEngine.MAX_LANDINGS, state.life(),
                state.board().rows(), pieceView(state.piece()));
    }

    @Transactional
    public LandingResponse land(UUID playerId, UUID gameId, LandingRequest request) {
        if (request == null || request.index() == null || request.x() == null
                || request.y() == null || request.rotation() == null) {
            throw new BadRequestException("Invalid landing");
        }
        Player player = players.lock(playerId);
        Game game = games.findByIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new NotFoundException("Game not found"));

        // 応答が届かず再送された着地には、前回と同じ応答を返す。
        String key = request.index() + ":" + request.x() + ":" + request.y() + ":" + request.rotation();
        if (request.index() == game.getLandingCount() - 1 && key.equals(game.getLastLandingKey())) {
            return readResponse(game.getLastLandingResponse());
        }
        if (game.getStatus() != Game.Status.IN_PROGRESS || request.index() != game.getLandingCount()) {
            throw new ConflictException("Landing does not match the game state");
        }

        GameState state = game.toState();
        LandingOutcome outcome = engine.land(state, request.x(), request.y(), request.rotation());

        List<WordResult> words = new ArrayList<>();
        Map<String, MeaningResponse> meanings = new LinkedHashMap<>();
        for (ScoredWord scored : outcome.words()) {
            MeaningResponse meaning = dictionary.getMeaning(scored.word());
            meanings.put(meaning.getWord(), meaning);
            words.add(new WordResult(meaning.getWord(), meaning.getPartOfSpeech(), meaning.getDefinition(),
                    scored.points(), scored.cells()));
        }
        player.recordLanding(words.size(), state.combo(),
                words.stream().anyMatch(w -> w.word().length() >= 4),
                words.stream().anyMatch(w -> w.word().length() >= 5));
        achievements.unlockReached(player, state.score());
        meanings.values().forEach(meaning -> wordLog.record(playerId, meaning));
        game.applyState(state, words.stream().map(WordResult::word).toList());

        GameResult result = outcome.endReason() == null ? null : finish(game, outcome.endReason());
        LandingResponse response = new LandingResponse(request.index(), words, outcome.multiWordBonus(),
                state.combo(), state.score(), state.wordCount(), state.life(), outcome.lifeLost(),
                state.board().rows(), result == null ? pieceView(state.piece()) : null, result);
        game.rememberLanding(key, writeResponse(response));
        return response;
    }

    /** プレイヤーが途中でやめたプレイを終了させる。既に終わっていれば何もしない。 */
    @Transactional
    public void quit(UUID playerId, UUID gameId) {
        players.lock(playerId);
        Game game = games.findByIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new NotFoundException("Game not found"));
        if (game.getStatus() == Game.Status.IN_PROGRESS) finish(game, EndReason.QUIT);
    }

    private GameResult finish(Game game, EndReason reason) {
        long previousHigh = games.findTopByPlayerIdAndStatusOrderByScoreDesc(game.getPlayerId(), Game.Status.FINISHED)
                .map(Game::getScore)
                .orElse(0L);
        game.finish(reason, previousHigh, LocalDateTime.now());
        return new GameResult(reason.name(), game.getScore(), game.getWordCount(),
                game.getHighScore(), game.isNewHighScore());
    }

    private PieceView pieceView(Piece piece) {
        return PieceView.of(piece, engine.isLucky(piece));
    }

    private String writeResponse(LandingResponse response) {
        try {
            return json.writeValueAsString(response);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException(error);
        }
    }

    private LandingResponse readResponse(String text) {
        try {
            return json.readValue(text, LandingResponse.class);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException(error);
        }
    }
}

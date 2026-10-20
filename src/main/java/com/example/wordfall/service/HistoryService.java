package com.example.wordfall.service;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wordfall.dto.PageResponse;
import com.example.wordfall.dto.ViewDtos;
import com.example.wordfall.entity.Game;
import com.example.wordfall.repository.GameRepository;

/** 終了したプレイの履歴とハイスコア。 */
@Service
public class HistoryService {

    private final GameRepository games;

    public HistoryService(GameRepository games) {
        this.games = games;
    }

    @Transactional(readOnly = true)
    public long highScore(UUID playerId) {
        return games.findTopByPlayerIdAndStatusOrderByScoreDesc(playerId, Game.Status.FINISHED)
                .map(Game::getScore)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public PageResponse<ViewDtos.ScoreEntry> list(UUID playerId, int page, int size) {
        return PageResponse.of(
                games.findByPlayerIdAndStatusOrderByFinishedAtDesc(playerId, Game.Status.FINISHED, PageRequest.of(page, size)),
                game -> new ViewDtos.ScoreEntry(game.getScore(), game.getWordCount(), game.getWords(), game.getFinishedAt()));
    }
}

package com.example.wordfall.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wordfall.dto.PageResponse;
import com.example.wordfall.dto.ViewDtos;
import com.example.wordfall.service.HistoryService;
import com.example.wordfall.web.PlayerCookieFilter;

@RestController
public class ScoreController {

    private final HistoryService historyService;

    public ScoreController(HistoryService historyService) {
        this.historyService = historyService;
    }

    // GET /api/score/high:自分の最高得点を返す。履歴が無ければ0
    @GetMapping("/api/score/high")
    public ViewDtos.HighScore getHighScore(@RequestAttribute(PlayerCookieFilter.PLAYER_ID) UUID playerId) {
        return new ViewDtos.HighScore(historyService.highScore(playerId));
    }

    // GET /api/score/list?page=0&size=5:自分のプレイ履歴を新しい順に返す
    @GetMapping("/api/score/list")
    public PageResponse<ViewDtos.ScoreEntry> getScoreList(
            @RequestAttribute(PlayerCookieFilter.PLAYER_ID) UUID playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Paging.validate(page, size, 50);
        return historyService.list(playerId, page, size);
    }
}

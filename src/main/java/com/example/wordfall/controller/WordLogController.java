package com.example.wordfall.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wordfall.dto.PageResponse;
import com.example.wordfall.dto.ViewDtos;
import com.example.wordfall.service.WordLogService;
import com.example.wordfall.web.PlayerCookieFilter;

@RestController
public class WordLogController {

    private final WordLogService wordLogService;

    public WordLogController(WordLogService wordLogService) {
        this.wordLogService = wordLogService;
    }

    // GET /api/word-log/list?page=0&size=10:自分の単語帳をアルファベット順に返す
    @GetMapping("/api/word-log/list")
    public PageResponse<ViewDtos.WordEntry> getWordLogList(
            @RequestAttribute(PlayerCookieFilter.PLAYER_ID) UUID playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Paging.validate(page, size, 100);
        return wordLogService.list(playerId, page, size);
    }
}

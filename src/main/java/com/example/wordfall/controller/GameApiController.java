package com.example.wordfall.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.wordfall.dto.GameDtos.LandingRequest;
import com.example.wordfall.dto.GameDtos.LandingResponse;
import com.example.wordfall.dto.GameDtos.StartResponse;
import com.example.wordfall.service.GameService;
import com.example.wordfall.web.PlayerCookieFilter;

// 書き込み系APIはJSONだけを受け付ける(他サイトのフォームから送らせないため)
@RestController
public class GameApiController {

    private final GameService gameService;

    public GameApiController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping(path = "/api/games", consumes = MediaType.APPLICATION_JSON_VALUE)
    public StartResponse start(@RequestAttribute(PlayerCookieFilter.PLAYER_ID) UUID playerId) {
        return gameService.start(playerId);
    }

    @PostMapping(path = "/api/games/{gameId}/landings", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LandingResponse land(@RequestAttribute(PlayerCookieFilter.PLAYER_ID) UUID playerId,
                                @PathVariable UUID gameId, @RequestBody LandingRequest request) {
        return gameService.land(playerId, gameId, request);
    }

    @PostMapping(path = "/api/games/{gameId}/quit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> quit(@RequestAttribute(PlayerCookieFilter.PLAYER_ID) UUID playerId,
                                     @PathVariable UUID gameId) {
        gameService.quit(playerId, gameId);
        return ResponseEntity.noContent().build();
    }
}

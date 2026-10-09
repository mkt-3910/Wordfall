package com.example.wordfall.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.example.wordfall.dto.ViewDtos;
import com.example.wordfall.service.AchievementService;
import com.example.wordfall.web.PlayerCookieFilter;

@RestController
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    // GET /api/achievements:全実績の一覧を、自分が解除済みかどうかの情報つきで返す
    @GetMapping("/api/achievements")
    public List<ViewDtos.Achievement> getAchievements(@RequestAttribute(PlayerCookieFilter.PLAYER_ID) UUID playerId) {
        return achievementService.getAchievements(playerId);
    }
}

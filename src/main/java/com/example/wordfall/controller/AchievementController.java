package com.example.wordfall.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.wordfall.entity.UnlockedAchievement;
import com.example.wordfall.model.AchievementDefinition;
import com.example.wordfall.repository.UnlockedAchievementRepository;
import com.example.wordfall.service.AchievementService;

@RestController
public class AchievementController {

    private final AchievementService achievementService;
    private final UnlockedAchievementRepository unlockedAchievementRepository;

    public AchievementController(AchievementService achievementService,
                                  UnlockedAchievementRepository unlockedAchievementRepository) {
        this.achievementService = achievementService;
        this.unlockedAchievementRepository = unlockedAchievementRepository;
    }

    // GET /api/achievements:全実績の一覧を、解除済みかどうかの情報つきで返す
    @GetMapping("/api/achievements")
    public List<AchievementResponse> getAchievements() {

        List<UnlockedAchievement> unlocked = unlockedAchievementRepository.findAll();

        Set<String> unlockedKeys = unlocked.stream()
                .map(UnlockedAchievement::getAchievementKey)
                .collect(Collectors.toSet());

        List<AchievementDefinition> definitions = achievementService.getAllDefinitions();

        return definitions.stream()
                .map(def -> new AchievementResponse(
                        def.getKey(),
                        def.getTitle(),
                        def.getDescription(),
                        unlockedKeys.contains(def.getKey())
                ))
                .collect(Collectors.toList());
    }

    // POST /api/achievements/check:1回のプレイが終わった時に呼び出し、実績判定を行う
    @PostMapping("/api/achievements/check")
    public void checkAchievements(@RequestBody AchievementCheckRequest request) {
        validate(request);
        achievementService.checkAndUnlock(
                request.getWordsCompletedThisGame(),
                request.getBestComboThisGame(),
                request.getBestMultiWordThisGame(),
                request.isGot4LetterThisGame(),
                request.isGot5LetterThisGame(),
                request.getFinalScore()
        );
    }

    private void validate(AchievementCheckRequest request) {
        if (request == null
                || request.getWordsCompletedThisGame() < 0
                || request.getWordsCompletedThisGame() > 1000
                || request.getBestComboThisGame() < 0
                || request.getBestComboThisGame() > request.getWordsCompletedThisGame()
                || request.getBestMultiWordThisGame() < 0
                || request.getBestMultiWordThisGame() > request.getWordsCompletedThisGame()
                || request.getFinalScore() < 0
                || request.getFinalScore() > 10_000_000
                || (request.isGot5LetterThisGame() && !request.isGot4LetterThisGame())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid game result");
        }
    }

    // レスポンス用のDTO(実績1つ分)
    public static class AchievementResponse {
        private final String key;
        private final String title;
        private final String description;
        private final boolean unlocked;

        public AchievementResponse(String key, String title, String description, boolean unlocked) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.unlocked = unlocked;
        }

        public String getKey() { return key; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean isUnlocked() { return unlocked; }
    }
}

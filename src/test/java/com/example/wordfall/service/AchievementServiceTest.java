package com.example.wordfall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.wordfall.dto.ViewDtos;
import com.example.wordfall.entity.Player;
import com.example.wordfall.repository.PlayerRepository;

@SpringBootTest
@Transactional
class AchievementServiceTest {

    @Autowired
    AchievementService service;

    @Autowired
    PlayerRepository players;

    @Test
    void unlocksEveryThresholdReachedOnceAndOnlyForThatPlayer() {
        Player player = players.save(new Player(UUID.randomUUID(), LocalDateTime.now()));
        Player other = players.save(new Player(UUID.randomUUID(), LocalDateTime.now()));
        for (int i = 0; i < 4; i++) player.recordLanding(2, i + 1, true, false);
        player.recordLanding(2, 5, false, false);

        service.unlockReached(player, 1000);
        service.unlockReached(player, 1000);

        assertEquals(Set.of("word_10", "first_4letter", "combo_5", "multi_2", "score_1000"), unlocked(player));
        assertTrue(unlocked(other).isEmpty());
        assertEquals(10, service.getAchievements(other.getId()).size());
    }

    private Set<String> unlocked(Player player) {
        return service.getAchievements(player.getId()).stream()
                .filter(ViewDtos.Achievement::unlocked)
                .map(ViewDtos.Achievement::key)
                .collect(Collectors.toSet());
    }
}

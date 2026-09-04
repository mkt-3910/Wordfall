package com.example.wordfall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.wordfall.entity.GameStats;
import com.example.wordfall.entity.UnlockedAchievement;
import com.example.wordfall.model.AchievementDefinition;
import com.example.wordfall.repository.GameStatsRepository;
import com.example.wordfall.repository.UnlockedAchievementRepository;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private UnlockedAchievementRepository unlockedAchievementRepository;

    private AchievementService service;

    @BeforeEach
    void setUp() {
        service = new AchievementService(gameStatsRepository, unlockedAchievementRepository);
    }

    @Test
    void unlocksEveryThresholdReachedByTheGame() {
        GameStats stats = new GameStats();
        stats.setTotalWordsCompleted(9);
        when(gameStatsRepository.findById(1L)).thenReturn(Optional.of(stats));
        when(unlockedAchievementRepository.existsByAchievementKey(anyString())).thenReturn(false);

        service.checkAndUnlock(1, 5, 2, true, false, 1000);

        assertEquals(10, stats.getTotalWordsCompleted());
        assertEquals(5, stats.getBestCombo());
        assertEquals(2, stats.getBestMultiWord());

        ArgumentCaptor<UnlockedAchievement> captor = ArgumentCaptor.forClass(UnlockedAchievement.class);
        verify(unlockedAchievementRepository, times(5)).save(captor.capture());
        Set<String> unlockedKeys = captor.getAllValues().stream()
                .map(UnlockedAchievement::getAchievementKey)
                .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(
                        "word_10", "first_4letter", "combo_5", "multi_2", "score_1000")),
                unlockedKeys);
    }

    @Test
    void definitionsCannotBeMutatedByCallers() {
        List<AchievementDefinition> definitions = service.getAllDefinitions();
        assertEquals(10, definitions.size());
        assertThrows(UnsupportedOperationException.class, definitions::clear);
    }
}

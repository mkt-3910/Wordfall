package com.example.wordfall.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.example.wordfall.entity.Score;
import com.example.wordfall.entity.WordLog;
import com.example.wordfall.repository.ScoreRepository;
import com.example.wordfall.repository.UnlockedAchievementRepository;
import com.example.wordfall.repository.WordLogRepository;
import com.example.wordfall.service.AchievementService;
import com.example.wordfall.service.DictionaryService;

class ControllerValidationTest {

    @Test
    void scoreRejectsInjectedWordsAndInvalidPagination() {
        ScoreRepository repository = mock(ScoreRepository.class);
        ScoreController controller = new ScoreController(repository);
        ScoreRequest request = scoreRequest(10, 1, "<script>alert(1)</script>");

        assertThrows(ResponseStatusException.class, () -> controller.saveScore(request));
        assertThrows(ResponseStatusException.class, () -> controller.getScoreList(-1, 5));
        assertThrows(ResponseStatusException.class, () -> controller.getScoreList(0, 51));
        verify(repository, never()).save(any());
    }

    @Test
    void scoreNormalizesAValidWordList() {
        ScoreRepository repository = mock(ScoreRepository.class);
        when(repository.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ScoreController controller = new ScoreController(repository);

        Score saved = controller.saveScore(scoreRequest(60, 2, "cat,dog"));

        assertEquals("CAT,DOG", saved.getWords());
    }

    @Test
    void wordLogRejectsMarkupAndNormalizesValidWords() {
        WordLogRepository repository = mock(WordLogRepository.class);
        DictionaryService dictionaryService = mock(DictionaryService.class);
        when(dictionaryService.hasGuaranteedMeaning("cat")).thenReturn(true);
        when(dictionaryService.getMeaning("CAT"))
                .thenReturn(new MeaningResponse("CAT", "名詞", "猫"));
        WordLogController controller = new WordLogController(repository, dictionaryService);
        WordLogRequest invalid = wordLogRequest("<img>", "noun", "意味");
        assertThrows(ResponseStatusException.class, () -> controller.saveWord(invalid));

        when(repository.findByWordIgnoreCase("CAT")).thenReturn(java.util.Optional.empty());
        controller.saveWord(wordLogRequest("cat", "改ざんされた品詞", "改ざんされた意味"));

        ArgumentCaptor<WordLog> captor = ArgumentCaptor.forClass(WordLog.class);
        verify(repository).save(captor.capture());
        assertEquals("CAT", captor.getValue().getWord());
        assertEquals("名詞", captor.getValue().getPartOfSpeech());
        assertEquals("猫", captor.getValue().getMeaning());
    }

    @Test
    void wordLogRepairsAnExistingPlaceholderMeaning() {
        WordLogRepository repository = mock(WordLogRepository.class);
        DictionaryService dictionaryService = mock(DictionaryService.class);
        WordLog existing = new WordLog(
                "WIN", "不明", "意味を取得できませんでした", LocalDateTime.now());
        when(dictionaryService.hasGuaranteedMeaning("win")).thenReturn(true);
        when(dictionaryService.getMeaning("WIN"))
                .thenReturn(new MeaningResponse("WIN", "動詞", "勝つ"));
        when(repository.findByWordIgnoreCase("WIN")).thenReturn(java.util.Optional.of(existing));

        new WordLogController(repository, dictionaryService)
                .saveWord(wordLogRequest("win", "不明", "意味を取得できませんでした"));

        verify(repository).save(existing);
        assertEquals("動詞", existing.getPartOfSpeech());
        assertEquals("勝つ", existing.getMeaning());
    }

    @Test
    void achievementsRejectImpossibleClientState() {
        AchievementService service = mock(AchievementService.class);
        AchievementController controller = new AchievementController(
                service, mock(UnlockedAchievementRepository.class));
        AchievementCheckRequest request = new AchievementCheckRequest();
        request.setWordsCompletedThisGame(1);
        request.setBestComboThisGame(2);

        assertThrows(ResponseStatusException.class, () -> controller.checkAchievements(request));
        verify(service, never()).checkAndUnlock(1, 2, 0, false, false, 0);
    }

    private ScoreRequest scoreRequest(int score, int wordCount, String words) {
        ScoreRequest request = new ScoreRequest();
        request.setScore(score);
        request.setWordCount(wordCount);
        request.setWords(words);
        return request;
    }

    private WordLogRequest wordLogRequest(String word, String partOfSpeech, String meaning) {
        WordLogRequest request = new WordLogRequest();
        request.setWord(word);
        request.setPartOfSpeech(partOfSpeech);
        request.setMeaning(meaning);
        return request;
    }
}

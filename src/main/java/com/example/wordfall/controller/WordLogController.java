package com.example.wordfall.controller;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.wordfall.entity.WordLog;
import com.example.wordfall.repository.WordLogRepository;
import com.example.wordfall.service.DictionaryService;

@RestController
public class WordLogController {

    private final WordLogRepository wordLogRepository;
    private final DictionaryService dictionaryService;

    public WordLogController(WordLogRepository wordLogRepository, DictionaryService dictionaryService) {
        this.wordLogRepository = wordLogRepository;
        this.dictionaryService = dictionaryService;
    }

    @PostMapping("/api/word-log")
    public void saveWord(@RequestBody WordLogRequest request) {
        validateWordLog(request);
        String normalizedWord = request.getWord().toUpperCase(Locale.ROOT);
        MeaningResponse dictionaryEntry = dictionaryService.getMeaning(normalizedWord);
        if (dictionaryEntry == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word has no guaranteed meaning");
        }

        Optional<WordLog> existing = wordLogRepository.findByWordIgnoreCase(normalizedWord);
        if (existing.isPresent()) {
            WordLog entry = existing.get();
            entry.updateMeaning(dictionaryEntry.getPartOfSpeech(), dictionaryEntry.getDefinition());
            wordLogRepository.save(entry);
            return;
        }

        WordLog newEntry = new WordLog(
                normalizedWord,
                dictionaryEntry.getPartOfSpeech(),
                dictionaryEntry.getDefinition(),
                LocalDateTime.now()
        );
        try {
            wordLogRepository.save(newEntry);
        } catch (DataIntegrityViolationException exception) {
            // 同時リクエストで先に同じ単語が保存された場合は成功扱いにする。
            if (!wordLogRepository.findByWordIgnoreCase(normalizedWord).isPresent()) {
                throw exception;
            }
        }
    }

    @GetMapping("/api/word-log/list")
    public Page<WordLog> getWordLogList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        Pageable pageable = PageRequest.of(page, size);
        return wordLogRepository.findAllByOrderByWordAsc(pageable);
    }

    private void validateWordLog(WordLogRequest request) {
        if (request == null || request.getWord() == null
                || !request.getWord().matches("[A-Za-z]{3,13}")
                || !dictionaryService.hasGuaranteedMeaning(request.getWord())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid word log");
        }
    }
}

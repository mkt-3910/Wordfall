package com.example.wordfall.controller;

import java.time.LocalDateTime;

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

@RestController
public class WordLogController {

    private final WordLogRepository wordLogRepository;

    public WordLogController(WordLogRepository wordLogRepository) {
        this.wordLogRepository = wordLogRepository;
    }

    @PostMapping("/api/word-log")
    public void saveWord(@RequestBody WordLogRequest request) {
        validateWordLog(request);
        String normalizedWord = request.getWord().toUpperCase();

        boolean alreadyExists = wordLogRepository.findByWordIgnoreCase(normalizedWord).isPresent();

        if (alreadyExists) {
            return;
        }

        WordLog newEntry = new WordLog(
                normalizedWord,
                request.getPartOfSpeech().trim(),
                request.getMeaning().trim(),
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
                || request.getPartOfSpeech() == null || request.getPartOfSpeech().length() > 50
                || request.getMeaning() == null || request.getMeaning().trim().isEmpty()
                || request.getMeaning().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid word log");
        }
    }
}

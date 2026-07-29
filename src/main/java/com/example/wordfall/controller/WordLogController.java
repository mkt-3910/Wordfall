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

        boolean alreadyExists = wordLogRepository.findByWord(request.getWord()).isPresent();

        if (alreadyExists) {
            return;
        }

        WordLog newEntry = new WordLog(
                request.getWord(),
                request.getPartOfSpeech(),
                request.getMeaning(),
                LocalDateTime.now()
        );
        wordLogRepository.save(newEntry);
    }

    @GetMapping("/api/word-log/list")
    public Page<WordLog> getWordLogList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return wordLogRepository.findAllByOrderByWordAsc(pageable);
    }
}

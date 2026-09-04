package com.example.wordfall.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.wordfall.entity.Score;
import com.example.wordfall.repository.ScoreRepository;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ScoreController {

    private final ScoreRepository scoreRepository;

    // コンストラクタインジェクション(Spring徹底入門でも扱ったDIの書き方)
    public ScoreController(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    // GET /api/score/high:今までの最高得点を返す。1件も無ければ、スコア0の空データを返す
    @GetMapping("/api/score/high")
    public Score getHighScore() {
        return scoreRepository.findTopByOrderByScoreDesc()
                .orElse(new Score(0, 0, "", null));
    }

    // POST /api/score:新しいプレイ結果を保存する
    @PostMapping("/api/score")
    public Score saveScore(@RequestBody ScoreRequest request) {
        validateScore(request);
        String normalizedWords = request.getWords() == null ? "" : request.getWords().toUpperCase();
        Score newScore = new Score(request.getScore(), request.getWordCount(), normalizedWords, LocalDateTime.now());
        return scoreRepository.save(newScore);
    }

    // GET /api/score/list?page=0&size=5:ページ送り可能な履歴一覧を返す
    @GetMapping("/api/score/list")
    public Page<Score> getScoreList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        validatePage(page, size, 50);
        Pageable pageable = PageRequest.of(page, size);
        return scoreRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private void validateScore(ScoreRequest request) {
        if (request == null || request.getScore() < 0 || request.getScore() > 10_000_000
                || request.getWordCount() < 0 || request.getWordCount() > 1000) {
            badRequest("Invalid score");
        }

        String words = request.getWords() == null ? "" : request.getWords().trim();
        String[] entries = words.isEmpty() ? new String[0] : words.split(",", -1);
        if (entries.length != request.getWordCount() || words.length() > 16_000) {
            badRequest("Word count does not match words");
        }
        for (String word : entries) {
            if (!word.matches("[A-Za-z]{3,13}")) {
                badRequest("Invalid word list");
            }
        }
    }

    private void validatePage(int page, int size, int maxSize) {
        if (page < 0 || size < 1 || size > maxSize) {
            badRequest("Invalid pagination");
        }
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

}

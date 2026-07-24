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
        Score newScore = new Score(request.getScore(), request.getWordCount(), request.getWords(), LocalDateTime.now());
        return scoreRepository.save(newScore);
    }

    // GET /api/score/list?page=0&size=5:ページ送り可能な履歴一覧を返す
    @GetMapping("/api/score/list")
    public Page<Score> getScoreList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return scoreRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

}

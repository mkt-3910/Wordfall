package com.example.wordfall.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.Score;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    // scoreカラムが一番大きい1件だけを取得する(Todoapiでの「派生クエリ」と同じ書き方)
    Optional<Score> findTopByOrderByScoreDesc();

    //新しい順で、指定ページのデータを取得する
    Page<Score> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

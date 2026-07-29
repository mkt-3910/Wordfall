package com.example.wordfall.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.WordLog;

public interface WordLogRepository extends JpaRepository<WordLog, Long> {

    Optional<WordLog> findByWord(String word);

    Page<WordLog> findAllByOrderByWordAsc(Pageable pageable);
}

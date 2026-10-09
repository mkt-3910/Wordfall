package com.example.wordfall.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.WordLog;

public interface WordLogRepository extends JpaRepository<WordLog, Long> {

    Optional<WordLog> findByPlayerIdAndWord(UUID playerId, String word);

    Page<WordLog> findByPlayerIdOrderByWordAsc(UUID playerId, Pageable pageable);
}

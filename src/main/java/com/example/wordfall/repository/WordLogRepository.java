package com.example.wordfall.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wordfall.entity.WordLog;

public interface WordLogRepository extends JpaRepository<WordLog, Long> {

}

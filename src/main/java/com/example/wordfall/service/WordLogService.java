package com.example.wordfall.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wordfall.dto.PageResponse;
import com.example.wordfall.dto.ViewDtos;
import com.example.wordfall.entity.WordLog;
import com.example.wordfall.model.MeaningResponse;
import com.example.wordfall.repository.WordLogRepository;

@Service
public class WordLogService {

    private final WordLogRepository repository;

    public WordLogService(WordLogRepository repository) {
        this.repository = repository;
    }

    /** 完成した単語を単語帳に入れる。呼び出し元でプレイヤー行をロックしておくこと(同じ単語の二重登録を防ぐ)。 */
    @Transactional
    public void record(UUID playerId, MeaningResponse meaning) {
        repository.findByPlayerIdAndWord(playerId, meaning.getWord()).ifPresentOrElse(
                entry -> entry.updateMeaning(meaning.getPartOfSpeech(), meaning.getDefinition()),
                () -> repository.save(new WordLog(playerId, meaning.getWord(), meaning.getPartOfSpeech(),
                        meaning.getDefinition(), LocalDateTime.now())));
    }

    @Transactional(readOnly = true)
    public PageResponse<ViewDtos.WordEntry> list(UUID playerId, int page, int size) {
        return PageResponse.of(repository.findByPlayerIdOrderByWordAsc(playerId, PageRequest.of(page, size)),
                entry -> new ViewDtos.WordEntry(entry.getWord(), entry.getPartOfSpeech(), entry.getMeaning()));
    }
}

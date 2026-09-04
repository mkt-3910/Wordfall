package com.example.wordfall.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.wordfall.service.DictionaryService;

@RestController
public class WordController {

    // 辞書・翻訳の処理を担当するService。コンストラクタインジェクションで受け取る
    private final DictionaryService dictionaryService;

    public WordController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    // GET /api/check-word?word=CAT のようにアクセスされたら、
    // 日本語の意味を保証できるローカル辞書に含まれているかを返す
    @GetMapping("/api/check-word")
    public boolean returnWords(@RequestParam String word) {
        return isKnownWord(word);
    }

    @PostMapping("/api/check-words")
    public Map<String, Boolean> returnWords(@RequestBody List<String> requestedWords) {
        if (requestedWords == null || requestedWords.size() > 4096) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many words");
        }

        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String word : requestedWords) {
            if (word == null || !word.matches("[A-Za-z]{3,13}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid word");
            }
            String normalized = word.toUpperCase(Locale.ROOT);
            result.put(normalized, dictionaryService.hasGuaranteedMeaning(normalized));
        }
        return result;
    }

    // GET /api/meaning?word=CAT:意味の取得はDictionaryServiceに丸ごと任せる
    @GetMapping("/api/meaning")
    public MeaningResponse getMeaning(@RequestParam String word) {
        if (!isKnownWord(word)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown word");
        }
        return dictionaryService.getMeaning(word.toUpperCase(Locale.ROOT));
    }

    private boolean isKnownWord(String word) {
        return word != null
                && word.matches("[A-Za-z]{3,13}")
                && dictionaryService.hasGuaranteedMeaning(word);
    }
}

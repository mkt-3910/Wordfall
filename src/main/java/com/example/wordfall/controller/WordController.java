package com.example.wordfall.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wordfall.service.DictionaryService;

@RestController
public class WordController {

    // 単語を保持しておく入れ物。検索が速いのでSetを使う
    private Set<String> words = new HashSet<>();

    // 辞書・翻訳の処理を担当するService。コンストラクタインジェクションで受け取る
    private final DictionaryService dictionaryService;

    public WordController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    // アプリ起動時に1回だけ呼ばれるメソッド。ここでwords.txtを読み込む
    @PostConstruct
    public void loadWords() throws IOException {
        ClassPathResource resource = new ClassPathResource("words.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim().toUpperCase());
            }
        }
    }

    // GET /api/check-word?word=CAT のようにアクセスされたら、
    // wordsセットにその単語(大文字にしたもの)が含まれているかをtrue/falseで返す
    @GetMapping("/api/check-word")
    public boolean returnWords(@RequestParam String word) {
        return words.contains(word.toUpperCase());
    }

    // GET /api/meaning?word=CAT:意味の取得はDictionaryServiceに丸ごと任せる
    @GetMapping("/api/meaning")
    public MeaningResponse getMeaning(@RequestParam String word) {
        return dictionaryService.getMeaning(word);
    }
}
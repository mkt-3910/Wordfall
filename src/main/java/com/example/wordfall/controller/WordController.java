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
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

// ↓ ここに@RestControllerを付ける(JSONを返すControllerだったことを思い出してください)
@RestController
public class WordController {

    // 単語を保持しておく入れ物。検索が速いのでSetを使う
    private Set<String> words = new HashSet<>();

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

    // ↓ ここにメソッドを書いてみてください
    // GET /api/check-word?word=CAT のようにアクセスされたら、
    // wordsセットにその単語(大文字にしたもの)が含まれているかをtrue/falseで返す
    @GetMapping("/api/check-word")
    public boolean returnWords(@RequestParam String word) {
        return words.contains(word.toUpperCase());
    }

    //外部の辞書APIを呼び出すための道具
    private final RestTemplate restTemplate = new RestTemplate();

    //JSON文字列を解析するための道具
    private final Object objectMapper = new ObjectMapper();

    @GetMapping("/api/meaning")
    public MeaningResponse getMeaning(@RequestParam String word) {
        try{
            //1.外部の辞書APIを呼び出す
            String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word.toLowerCase();
            String json = restTemplate.getForObject(url,String.class);

            // 2.JSON文字列を解析する
            JsonNode root = objectMapper.readTree(json);
            JsonNode firstEntry = firstEntry.get("meanings").get(0);
            String partOfSpeech = firstMeaning.get("partOfSpeech").asText();
            String definition = firstMeaning.get("definitions").get(0).get("definition").asText();

            //3.シンプルな形にして返す
            return new MeaningResponse(word.toUpperCase(), partOfSpeech, definition)
        }
    }

}

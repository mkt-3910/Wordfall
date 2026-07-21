package com.example.wordfall.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    // GET /api/check-word?word=CAT のようにアクセスされたら、
    // wordsセットにその単語(大文字にしたもの)が含まれているかをtrue/falseで返す
    @GetMapping("/api/check-word")
    public boolean returnWords(@RequestParam String word) {
        return words.contains(word.toUpperCase());
    }

    // 外部の辞書APIを呼び出すための道具
    private final RestTemplate restTemplate = new RestTemplate();

    // JSON文字列を解析するための道具
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/api/meaning")
    public MeaningResponse getMeaning(@RequestParam String word) {
        String lowerWord = word.toLowerCase();

        // 品詞取得と翻訳は、それぞれ独立して試す(片方が失敗しても、もう片方は諦めない)
        String partOfSpeechJa = translatePartOfSpeech(fetchPartOfSpeech(lowerWord));
        String meaningJa = translateWord(lowerWord);

        return new MeaningResponse(word.toUpperCase(), partOfSpeechJa, meaningJa);
    }

    // 辞書API(dictionaryapi.dev)から品詞だけを取り出す。失敗したらnullを返す(例外は投げない)
    private String fetchPartOfSpeech(String word) {
        try {
            String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode firstEntry = root.get(0);
            JsonNode firstMeaning = firstEntry.get("meanings").get(0);
            return firstMeaning.get("partOfSpeech").asText();
        } catch (Exception e) {
            return null; // 取れなくても、致命的なエラーにはしない
        }
    }

    // 品詞を日本語に変換する(よく出るものだけ対応)
    private String translatePartOfSpeech(String pos) {
        if (pos == null) {
            return null;
        }
        switch (pos) {
            case "noun":
                return "名詞";
            case "verb":
                return "動詞";
            case "adjective":
                return "形容詞";
            case "adverb":
                return "副詞";
            case "pronoun":
                return "代名詞";
            case "preposition":
                return "前置詞";
            case "conjunction":
                return "接続詞";
            case "interjection":
                return "感嘆詞";
            default:
                return pos;
        }
    }

    // 単語を日本語に翻訳する。まず英日辞書(Jisho)を試し、ダメなら機械翻訳に頼る
    private String translateWord(String word) {
        String result = fetchFromJisho(word);
        if (result != null) {
            return result;
        }

        result = translateViaMyMemory(word);
        if (result != null) {
            return result;
        }

        result = translateViaGoogle(word);
        if (result != null) {
            return result;
        }

        return "-";
    }

    // Jisho.org の辞書API(JMdictという、日本語⇔英語の対訳辞書データを使っている)から、
    // その英単語に対応する日本語の単語を探す
    // Jisho.org の辞書API(JMdictという、日本語⇔英語の対訳辞書データを使っている)から、
    // その英単語に対応する日本語の単語を探す
    private String fetchFromJisho(String word) {
        try {
            String encoded = java.net.URLEncoder.encode(word, "UTF-8");
            String url = "https://jisho.org/api/v1/search/words?keyword=" + encoded;

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");
            if (data == null || data.size() == 0) {
                return null;
            }

            // 英語の意味が「その単語ぴったり」と一致する候補だけを採用する。
            // ぴったり一致が無ければ、Jishoが「日本語の読み方」として拾ってきただけの
            // 誤マッチの可能性が高いので、無理に採用しない
            for (JsonNode entry : data) {
                JsonNode senses = entry.get("senses");
                if (senses == null) {
                    continue;
                }
                for (JsonNode sense : senses) {
                    JsonNode defs = sense.get("english_definitions");
                    if (defs == null) {
                        continue;
                    }
                    for (JsonNode def : defs) {
                        if (def.asText().equalsIgnoreCase(word)) {
                            return buildJapaneseText(entry);
                        }
                    }
                }
            }
            return null; // ぴったり一致が無い → 諦めて機械翻訳にバトンタッチする

        } catch (Exception e) {
            return null;
        }
    }

    // Jishoのエントリ1件から、日本語表記(漢字+読み方)の文字列を組み立てる
    private String buildJapaneseText(JsonNode entry) {
        JsonNode japaneseArr = entry.get("japanese");
        if (japaneseArr == null || japaneseArr.size() == 0) {
            return null;
        }

        JsonNode first = japaneseArr.get(0);
        String kanji = (first.hasNonNull("word")) ? first.get("word").asText() : null;
        String reading = (first.hasNonNull("reading")) ? first.get("reading").asText() : null;

        if (kanji != null && reading != null && !kanji.equals(reading)) {
            return kanji + "(" + reading + ")";
        } else if (kanji != null) {
            return kanji;
        } else if (reading != null) {
            return reading;
        }
        return null;
    }

    // 翻訳2:MyMemory
    private String translateViaMyMemory(String word) {
        try {
            String encoded = java.net.URLEncoder.encode(word, "UTF-8");
            String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=en|ja";

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            String translated = root.get("responseData").get("translatedText").asText();

            if (translated == null || translated.toUpperCase().contains("MYMEMORY WARNING")) {
                return null;
            }
            return translated;
        } catch (Exception e) {
            return null;
        }
    }

    // 翻訳3:Google翻訳
    private String translateViaGoogle(String word) {
        try {
            String encoded = java.net.URLEncoder.encode(word, "UTF-8");
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ja&dt=t&q=" + encoded;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get(0).get(0).get(0).asText();
        } catch (Exception e) {
            return null;
        }
    }
}

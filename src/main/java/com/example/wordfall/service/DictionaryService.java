package com.example.wordfall.service;

import java.net.URLEncoder;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.wordfall.controller.MeaningResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DictionaryService {

    // 辞書APIを呼び出すためのクラス
    private final RestTemplate restTemplate = new RestTemplate();

    // JSON解析用
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 英単語の意味を取得する
     */
    public MeaningResponse getMeaning(String word) {

        String lowerWord = word.toLowerCase();

        // 品詞取得
        String partOfSpeech
                = fetchPartOfSpeech(lowerWord);

        String partOfSpeechJa
                = translatePartOfSpeech(partOfSpeech);

        // 日本語訳取得
        String meaning
                = translateWord(lowerWord);

        // 日本語でなければ無効
        if (!isJapanese(meaning)) {
            return null;
        }

        return new MeaningResponse(
                word.toUpperCase(),
                partOfSpeechJa,
                meaning
        );
    }

    /**
     * dictionaryapi.devから品詞取得
     */
    private String fetchPartOfSpeech(String word) {

        try {

            String url
                    = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;

            HttpHeaders headers = new HttpHeaders();
            headers.set(
                    "User-Agent",
                    "Mozilla/5.0"
            );

            HttpEntity<String> entity
                    = new HttpEntity<>(headers);

            ResponseEntity<String> response
                    = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

            JsonNode root
                    = objectMapper.readTree(response.getBody());

            JsonNode firstEntry
                    = root.get(0);

            JsonNode firstMeaning
                    = firstEntry.get("meanings").get(0);

            return firstMeaning
                    .get("partOfSpeech")
                    .asText();

        } catch (Exception e) {

            return null;

        }
    }

    /**
     * 品詞を日本語へ変換
     */
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

    /**
     * 日本語訳取得 優先順位 ① Jisho ② MyMemory ③ Google翻訳
     */
    private String translateWord(String word) {

        String result
                = fetchFromJisho(word);

        if (result != null) {
            return result;
        }

        result
                = translateViaMyMemory(word);

        if (result != null) {
            return result;
        }

        result
                = translateViaGoogle(word);

        if (result != null) {
            return result;
        }

        return null;
    }

    /**
     * 日本語か判定
     */
    private boolean isJapanese(String text) {

        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        return text.matches(".*[ぁ-んァ-ヶ一-龯].*");
    }

    /**
     * Jisho APIから日本語訳を取得
     */
    private String fetchFromJisho(String word) {

        try {

            String encoded
                    = URLEncoder.encode(word, "UTF-8");

            String url
                    = "https://jisho.org/api/v1/search/words?keyword=" + encoded;

            String json
                    = restTemplate.getForObject(url, String.class);

            JsonNode root
                    = objectMapper.readTree(json);

            JsonNode data
                    = root.get("data");

            if (data == null || data.size() == 0) {
                return null;
            }

            // 英語の意味が完全一致するものだけ採用
            for (JsonNode entry : data) {

                JsonNode senses
                        = entry.get("senses");

                if (senses == null) {
                    continue;
                }

                for (JsonNode sense : senses) {

                    JsonNode defs
                            = sense.get("english_definitions");

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

            return null;

        } catch (Exception e) {

            return null;

        }
    }

    /**
     * 日本語表記を組み立てる
     */
    private String buildJapaneseText(JsonNode entry) {

        JsonNode japanese
                = entry.get("japanese");

        if (japanese == null || japanese.size() == 0) {
            return null;
        }

        JsonNode first
                = japanese.get(0);

        String kanji
                = first.hasNonNull("word")
                ? first.get("word").asText()
                : null;

        String reading
                = first.hasNonNull("reading")
                ? first.get("reading").asText()
                : null;

        if (kanji != null
                && reading != null
                && !kanji.equals(reading)) {

            return kanji + "(" + reading + ")";
        }

        if (kanji != null) {
            return kanji;
        }

        return reading;
    }

    /**
     * MyMemory翻訳
     */
    private String translateViaMyMemory(String word) {

        try {

            String encoded
                    = URLEncoder.encode(word, "UTF-8");

            String url
                    = "https://api.mymemory.translated.net/get?q="
                    + encoded
                    + "&langpair=en|ja";

            String json
                    = restTemplate.getForObject(url, String.class);

            JsonNode root
                    = objectMapper.readTree(json);

            String translated
                    = root.get("responseData")
                            .get("translatedText")
                            .asText();

            if (translated == null
                    || translated.toUpperCase().contains("MYMEMORY WARNING")) {

                return null;
            }

            return translated;

        } catch (Exception e) {

            return null;

        }
    }

    /**
     * Google翻訳(API)
     */
    private String translateViaGoogle(String word) {

        try {

            String encoded
                    = URLEncoder.encode(word, "UTF-8");

            String url
                    = "https://translate.googleapis.com/translate_a/single"
                    + "?client=gtx"
                    + "&sl=en"
                    + "&tl=ja"
                    + "&dt=t"
                    + "&q=" + encoded;

            HttpHeaders headers
                    = new HttpHeaders();

            headers.set(
                    "User-Agent",
                    "Mozilla/5.0"
            );

            HttpEntity<String> entity
                    = new HttpEntity<>(headers);

            ResponseEntity<String> response
                    = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

            JsonNode root
                    = objectMapper.readTree(response.getBody());

            return root
                    .get(0)
                    .get(0)
                    .get(0)
                    .asText();

        } catch (Exception e) {

            return null;

        }
    }

}

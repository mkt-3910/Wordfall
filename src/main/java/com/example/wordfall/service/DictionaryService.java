package com.example.wordfall.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.wordfall.controller.MeaningResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// @Service: このクラスが「業務ロジックを担当する部品」であることを示すアノテーション。
// @Controllerや@RestControllerと同じく、Springが自動的にインスタンスを作って管理してくれる
@Service
public class DictionaryService {

    // 外部の辞書APIを呼び出すための道具
    private final RestTemplate restTemplate = new RestTemplate();

    // JSON文字列を解析するための道具
    private final ObjectMapper objectMapper = new ObjectMapper();

    // application.propertiesに書いたDeepLのAPIキーを、ここに自動で読み込んでもらう
    @Value("${deepl.api.key}")
    private String deeplApiKey;

    // 単語1つから、意味の情報(品詞+日本語訳)をまとめて組み立てる、このクラスの入り口メソッド
    public MeaningResponse getMeaning(String word) {
        String lowerWord = word.toLowerCase();

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
            return null;
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

    // 単語を日本語に翻訳する。辞書(Jisho)→DeepL→MyMemory→Googleの順で試す
    private String translateWord(String word) {
        String result = fetchFromJisho(word);
        if (result != null) {
            return result;
        }

        result = translateViaDeepL(word);
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
            return null;

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

    // 翻訳元:DeepL API(精度の高い翻訳サービス)
    private String translateViaDeepL(String word) {
        try {
            String url = "https://api-free.deepl.com/v2/translate";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "DeepL-Auth-Key " + deeplApiKey);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String body = "text=" + java.net.URLEncoder.encode(word, "UTF-8")
                    + "&source_lang=EN&target_lang=JA";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode translations = root.get("translations");
            if (translations == null || translations.size() == 0) {
                return null;
            }

            return translations.get(0).get("text").asText();
        } catch (Exception e) {
            return null;
        }
    }

    // 翻訳元:MyMemory(無料翻訳API)
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

    // 翻訳元:Google翻訳(非公式の無料エンドポイント)
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
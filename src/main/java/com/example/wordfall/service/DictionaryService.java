package com.example.wordfall.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.example.wordfall.model.MeaningResponse;
import com.example.wordfall.model.DictionaryWord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** ゲームの成立判定と意味取得は、検証済みの同梱辞書だけを使う。 */
@Service
public class DictionaryService {
    private static final Pattern WORD = Pattern.compile("[A-Za-z]{3,13}");
    private static final Pattern JAPANESE = Pattern.compile("[ぁ-んァ-ヶ一-龯]");
    private static final Map<String, String> PARTS = Map.of(
            "noun", "名詞", "verb", "動詞", "adjective", "形容詞", "adverb", "副詞",
            "pronoun", "代名詞", "preposition", "前置詞", "conjunction", "接続詞",
            "determiner", "限定詞", "interjection", "感嘆詞");
    private final Map<String, MeaningResponse> meanings;

    public DictionaryService(ObjectMapper mapper) {
        try (InputStream input = new ClassPathResource("dictionary.json").getInputStream()) {
            meanings = validateEntries(mapper.readValue(input, new TypeReference<List<DictionaryWord>>() { }));
        } catch (IOException error) {
            throw new IllegalStateException("Failed to load bundled dictionary", error);
        }
    }

    static Map<String, MeaningResponse> validateEntries(List<DictionaryWord> entries) {
        if (entries == null) throw new IllegalStateException("Dictionary must be an array");
        Map<String, MeaningResponse> result = new HashMap<>();
        for (DictionaryWord entry : entries) {
            if (entry == null) throw new IllegalStateException("Null dictionary entry");
            if (!entry.isEnabled()) continue;
            String word = entry.getWord() == null ? "" : entry.getWord().trim().toUpperCase(Locale.ROOT);
            String meaning = entry.getMeaning() == null ? "" : entry.getMeaning().trim();
            String pos = entry.getPartOfSpeech();
            if (!WORD.matcher(word).matches() || entry.getLength() != word.length()
                    || meaning.length() > 1000 || !JAPANESE.matcher(meaning).find()
                    || pos == null || !PARTS.containsKey(pos)) {
                throw new IllegalStateException("Invalid enabled dictionary entry: " + word);
            }
            if (result.putIfAbsent(word, new MeaningResponse(word, PARTS.get(pos), meaning)) != null) {
                throw new IllegalStateException("Duplicate dictionary word: " + word);
            }
        }
        if (result.isEmpty()) throw new IllegalStateException("Bundled dictionary has no valid entries");
        return Map.copyOf(result);
    }

    public boolean hasGuaranteedMeaning(String word) {
        return getMeaning(word) != null;
    }

    public MeaningResponse getMeaning(String word) {
        if (word == null || !WORD.matcher(word).matches()) return null;
        return meanings.get(word.toUpperCase(Locale.ROOT));
    }
}

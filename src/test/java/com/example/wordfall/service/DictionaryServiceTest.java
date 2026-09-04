package com.example.wordfall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.example.wordfall.controller.MeaningResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class DictionaryServiceTest {

    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        dictionaryService = new DictionaryService(new RestTemplateBuilder(), new ObjectMapper());
    }

    @Test
    void bundledWordsHaveJapaneseMeaningsWithoutExternalApi() {
        assertMeaning("CAT", "名詞", "猫");
        assertMeaning("BOOK", "名詞", "本");
        assertMeaning("LEARN", "動詞", "学ぶ、習得する");
    }

    @Test
    void onlyWordsWithGuaranteedMeaningsAreAccepted() {
        assertTrue(dictionaryService.hasGuaranteedMeaning("cat"));
        assertTrue(dictionaryService.hasGuaranteedMeaning("APPLE"));
        assertFalse(dictionaryService.hasGuaranteedMeaning("ZZZ"));
        assertFalse(dictionaryService.hasGuaranteedMeaning("<script>"));
    }

    private void assertMeaning(String word, String partOfSpeech, String definition) {
        MeaningResponse response = dictionaryService.getMeaning(word);
        assertEquals(word, response.getWord());
        assertEquals(partOfSpeech, response.getPartOfSpeech());
        assertEquals(definition, response.getDefinition());
    }
}

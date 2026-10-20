package com.example.wordfall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.wordfall.model.MeaningResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class DictionaryServiceTest {

    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        dictionaryService = new DictionaryService(new ObjectMapper());
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

    @Test
    void unknownWordsNeverNeedExternalServices() {
        org.junit.jupiter.api.Assertions.assertNull(dictionaryService.getMeaning("ZZZZZZZ"));
        org.junit.jupiter.api.Assertions.assertNull(dictionaryService.getMeaning(null));
    }

    @Test
    void rejectsDuplicateAndMalformedEnabledEntries() {
        var entry = new com.example.wordfall.model.DictionaryWord();
        entry.setWord("CAT"); entry.setMeaning("猫"); entry.setPartOfSpeech("noun");
        entry.setLength(3); entry.setEnabled(true);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> DictionaryService.validateEntries(java.util.List.of(entry, entry)));
        entry.setLength(4);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> DictionaryService.validateEntries(java.util.List.of(entry)));
        entry.setLength(3); entry.setMeaning("ー");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> DictionaryService.validateEntries(java.util.List.of(entry)));
    }

    private void assertMeaning(String word, String partOfSpeech, String definition) {
        MeaningResponse response = dictionaryService.getMeaning(word);
        assertEquals(word, response.getWord());
        assertEquals(partOfSpeech, response.getPartOfSpeech());
        assertEquals(definition, response.getDefinition());
    }
}

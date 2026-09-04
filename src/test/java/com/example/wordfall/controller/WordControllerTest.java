package com.example.wordfall.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.example.wordfall.service.DictionaryService;

class WordControllerTest {

    private WordController controller;

    @BeforeEach
    void setUp() throws IOException {
        controller = new WordController(mock(DictionaryService.class));
        controller.loadWords();
    }

    @Test
    void checksManyWordsInOneRequest() {
        Map<String, Boolean> result = controller.returnWords(Arrays.asList("cat", "dog", "zzzzzzzzzzzzz"));

        assertTrue(result.get("CAT"));
        assertTrue(result.get("DOG"));
        assertFalse(result.get("ZZZZZZZZZZZZZ"));
    }

    @Test
    void rejectsMalformedWordsWithoutCallingExternalServices() {
        assertFalse(controller.returnWords("<script>"));
        assertThrows(ResponseStatusException.class,
                () -> controller.returnWords(Arrays.asList("CAT", "<script>")));
        assertThrows(ResponseStatusException.class, () -> controller.getMeaning("not-in-dictionary"));
    }
}

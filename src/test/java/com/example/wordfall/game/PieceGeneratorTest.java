package com.example.wordfall.game;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.example.wordfall.service.DictionaryService;
import com.fasterxml.jackson.databind.ObjectMapper;

class PieceGeneratorTest {

    @Test
    void seededWordsAllHaveGuaranteedMeanings() {
        DictionaryService dictionary = new DictionaryService(new ObjectMapper());
        Stream.concat(PieceGenerator.COMMON_THREE_LETTER_WORDS.stream(), PieceGenerator.COMMON_FOUR_LETTER_WORDS.stream())
                .forEach(word -> assertTrue(dictionary.hasGuaranteedMeaning(word), word + " must be in the dictionary"));
    }

    @Test
    void generatedPiecesUseOnlyUppercaseLetters() {
        Rng rng = new Rng(7);
        for (int i = 0; i < 1000; i++) {
            Piece piece = PieceGenerator.next(rng);
            assertTrue(piece.letters().chars().allMatch(ch -> ch >= 'A' && ch <= 'Z'), piece.letters());
        }
    }
}

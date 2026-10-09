package com.example.wordfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.example.wordfall.service.DictionaryService;
import com.fasterxml.jackson.databind.ObjectMapper;

class PieceGeneratorTest {

    private static final int SAMPLES = 20_000;

    private final DictionaryService dictionary = new DictionaryService(new ObjectMapper());
    private final WordFinder finder = new WordFinder(dictionary::hasGuaranteedMeaning);
    private final PieceGenerator generator = new PieceGenerator(finder);

    @Test
    void luckyWordsAndHintWordsAllHaveGuaranteedMeanings() {
        Stream.concat(PieceGenerator.COMMON_THREE_LETTER_WORDS.stream(), PieceGenerator.COMMON_FOUR_LETTER_WORDS.stream())
                .forEach(word -> assertTrue(dictionary.hasGuaranteedMeaning(word), word + " must be in the dictionary"));
    }

    @Test
    void luckyPiecesAreRare() {
        Rng rng = new Rng(2026);
        int lucky = 0;
        for (int i = 0; i < SAMPLES; i++) {
            if (finder.containsWord(generator.next(rng))) lucky++;
        }
        double rate = (double) lucky / SAMPLES;
        // I/T/J/L(7種中4種)の5% ≒ 2.9%
        assertTrue(rate > 0.015 && rate < 0.05, "lucky rate was " + rate);
    }

    @Test
    void ordinaryPiecesAreWordFriendlyButLeaveTheWordToThePlayer() {
        Rng rng = new Rng(7);
        for (int i = 0; i < SAMPLES; i++) {
            Piece piece = generator.next(rng);
            assertTrue(piece.letters().chars().allMatch(ch -> ch >= 'A' && ch <= 'Z'), piece.letters());
            if (finder.containsWord(piece)) continue;
            long vowels = piece.letters().chars().filter(ch -> PieceGenerator.isVowel((char) ch)).count();
            assertTrue(vowels >= 1 && vowels <= 2, piece.letters() + " should have 1-2 vowels");
        }
    }

    @Test
    void sameSeedProducesSamePieces() {
        Rng first = new Rng(123);
        Rng second = new Rng(123);
        for (int i = 0; i < 1000; i++) {
            assertEquals(generator.next(first), generator.next(second));
        }
        assertEquals(first.state(), second.state());
    }
}

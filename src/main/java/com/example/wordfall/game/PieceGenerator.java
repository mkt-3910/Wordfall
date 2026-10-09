package com.example.wordfall.game;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 乱数からミノを作る。
 * ・ふつうのミノは単語を作りやすい文字(母音1〜2個、ときどき単語の頭か終わりの2文字)にするが、
 *   ミノだけで単語が完成しないようにする(単語はプレイヤーが組み合わせて作る)
 * ・低い確率で、最初から単語が完成している「ラッキーミノ」を出す
 */
public final class PieceGenerator {

    private static final Shape[] SHAPES = Shape.values();

    private static final String VOWELS = "E".repeat(12) + "A".repeat(9) + "I".repeat(8) + "O".repeat(8) + "U".repeat(3);
    private static final String CONSONANTS = "T".repeat(9) + "N".repeat(8) + "S".repeat(8) + "R".repeat(8)
            + "L".repeat(5) + "D".repeat(5) + "C".repeat(4) + "M".repeat(4) + "P".repeat(3) + "B".repeat(3)
            + "G".repeat(3) + "H".repeat(3) + "W".repeat(2) + "Y".repeat(2) + "F".repeat(2) + "K".repeat(2)
            + "VXJZ";

    /** 単語の形をしたミノが出られる形(I/T/J/L)のうち、ラッキーミノになる確率。全体では約3%。 */
    static final double LUCKY_CHANCE = 0.05;
    /** ふつうのミノに「単語の頭か終わりの2文字」を入れる確率。 */
    static final double FRAGMENT_CHANCE = 0.5;
    /** 偶然単語ができてしまったミノを作り直す上限回数。 */
    private static final int MAX_ATTEMPTS = 50;

    static final List<String> COMMON_THREE_LETTER_WORDS = List.of(
            "ACT", "ADD", "AGE", "AIR", "ALL", "AND", "ANT", "ANY", "ARM", "ART",
            "ASK", "BAG", "BAT", "BED", "BEE", "BIG", "BOX", "BOY", "BUS", "CAR",
            "CAT", "CUP", "DAY", "DOG", "EAR", "EAT", "EGG", "FAN", "FOX", "FUN",
            "HAT", "ICE", "KEY", "MAN", "MAP", "PEN", "PIG", "RED", "RUN", "SEA",
            "SIT", "SUN", "TOP", "TOY", "WIN", "APE", "APP", "ACE", "AID", "AIM",
            "BAD", "BAR", "BIT", "BUY", "CAN", "CAP", "COW", "CRY", "CUT", "DAD",
            "DIE", "DRY", "END", "EYE", "FAR", "FAT", "FEW", "FLY", "GET", "GOD",
            "GUN", "GUY", "GYM", "HIT", "HOT", "HOW", "JOB", "JOY", "KID", "LEG",
            "LIE", "LIP", "LOT", "LOW", "MAY", "MOM", "NEW", "NOT", "NOW", "NUT",
            "OLD", "ONE", "OWN", "PAY", "PUT", "RAW", "SAD", "SAY", "SEE", "SET",
            "SKY", "SON", "TEA", "TEN", "TWO", "USE", "WAR", "WAY", "WEB", "WET",
            "WHY", "YES", "YET", "ZOO");

    static final List<String> COMMON_FOUR_LETTER_WORDS = List.of(
            "BOOK", "GAME", "WORD", "PLAY", "READ", "BLUE", "HOME", "LOVE", "TIME", "TREE",
            "ABLE", "BABY", "BALL", "BIRD", "BOAT", "CAKE", "CALL", "CARD", "CITY", "COOK",
            "EASY", "FACE", "FARM", "FIRE", "FISH", "FOOD", "GIRL", "GOOD", "HAND", "HELP",
            "HOPE", "JUMP", "LIFE", "MAKE", "MILK", "MOON", "RAIN", "RICE", "ROAD", "ROOM",
            "SHOP", "SING", "SNOW", "SONG", "STAR", "TEAM", "WALK", "WARM", "WASH", "WISH");

    private static final List<String> HINT_WORDS =
            Stream.concat(COMMON_THREE_LETTER_WORDS.stream(), COMMON_FOUR_LETTER_WORDS.stream()).toList();

    /** 形ごとの、隣り合う2マス(左→右、上→下)の組み。単語のかけらを置く場所の候補。 */
    private static final Map<Shape, List<int[]>> ADJACENT_PAIRS = adjacentPairs();

    private final WordFinder words;

    public PieceGenerator(WordFinder words) {
        this.words = words;
    }

    public Piece next(Rng rng) {
        Shape shape = SHAPES[rng.nextInt(SHAPES.length)];
        if (!shape.wordLines().isEmpty() && rng.next() < LUCKY_CHANCE) {
            return lucky(shape, rng);
        }
        Piece piece;
        int attempts = 0;
        do {
            piece = new Piece(shape, new String(wordFriendlyLetters(shape, rng)));
        } while (words.containsWord(piece) && ++attempts < MAX_ATTEMPTS);
        return piece;
    }

    /** ミノの一直線に、よく使う単語をまるごと入れる。 */
    private Piece lucky(Shape shape, Rng rng) {
        char[] letters = wordFriendlyLetters(shape, rng);
        boolean fourLetters = shape == Shape.I && rng.next() < 0.35;
        List<String> list = fourLetters ? COMMON_FOUR_LETTER_WORDS : COMMON_THREE_LETTER_WORDS;
        String word = list.get(rng.nextInt(list.size()));
        int[][] line = fourLetters ? Shape.FOUR_LETTER_LINE : shape.wordLines().get(rng.nextInt(shape.wordLines().size()));
        for (int i = 0; i < line.length; i++) {
            letters[indexOf(shape.cells(), line[i])] = word.charAt(i);
        }
        return new Piece(shape, new String(letters));
    }

    /** 母音を1〜2個にし、ときどき単語の頭か終わりの2文字を隣り合わせに入れる。 */
    private static char[] wordFriendlyLetters(Shape shape, Rng rng) {
        char[] letters = new char[shape.cells().length];
        if (rng.next() < FRAGMENT_CHANCE) {
            List<int[]> pairs = ADJACENT_PAIRS.get(shape);
            int[] pair = pairs.get(rng.nextInt(pairs.size()));
            String word = HINT_WORDS.get(rng.nextInt(HINT_WORDS.size()));
            String fragment = rng.next() < 0.5 ? word.substring(0, 2) : word.substring(word.length() - 2);
            letters[pair[0]] = fragment.charAt(0);
            letters[pair[1]] = fragment.charAt(1);
        }

        List<Integer> empty = new ArrayList<>();
        int vowels = 0;
        for (int i = 0; i < letters.length; i++) {
            if (letters[i] == 0) empty.add(i);
            else if (isVowel(letters[i])) vowels++;
        }
        int targetVowels = Math.max(vowels, 1 + rng.nextInt(2));
        int needed = Math.min(targetVowels - vowels, empty.size());
        for (int i = empty.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Integer swap = empty.get(i);
            empty.set(i, empty.get(j));
            empty.set(j, swap);
        }
        for (int i = 0; i < empty.size(); i++) {
            String pool = i < needed ? VOWELS : CONSONANTS;
            letters[empty.get(i)] = pool.charAt(rng.nextInt(pool.length()));
        }
        return letters;
    }

    static boolean isVowel(char letter) {
        return "AEIOU".indexOf(letter) >= 0;
    }

    private static int indexOf(int[][] cells, int[] target) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i][0] == target[0] && cells[i][1] == target[1]) return i;
        }
        throw new IllegalStateException("Word line is outside the piece");
    }

    private static Map<Shape, List<int[]>> adjacentPairs() {
        Map<Shape, List<int[]>> result = new EnumMap<>(Shape.class);
        for (Shape shape : SHAPES) {
            int[][] cells = shape.cells();
            List<int[]> pairs = new ArrayList<>();
            for (int a = 0; a < cells.length; a++) {
                for (int b = 0; b < cells.length; b++) {
                    boolean right = cells[b][0] == cells[a][0] + 1 && cells[b][1] == cells[a][1];
                    boolean below = cells[b][0] == cells[a][0] && cells[b][1] == cells[a][1] + 1;
                    if (right || below) pairs.add(new int[] {a, b});
                }
            }
            result.put(shape, List.copyOf(pairs));
        }
        return result;
    }
}

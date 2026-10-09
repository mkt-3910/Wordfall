package com.example.wordfall.game;

import java.util.List;

/** 乱数からミノを作る。文字の出やすさと「ときどき単語を仕込む」仕様をここに集約する。 */
public final class PieceGenerator {

    private static final Shape[] SHAPES = Shape.values();

    private static final String LETTER_POOL = "E".repeat(14) + "A".repeat(11) + "I".repeat(10) + "O".repeat(8)
            + "N".repeat(8) + "T".repeat(8) + "S".repeat(8) + "R".repeat(7)
            + "L".repeat(5) + "D".repeat(5) + "U".repeat(3) + "C".repeat(3)
            + "M".repeat(3) + "G".repeat(2) + "H".repeat(2) + "B".repeat(2)
            + "P".repeat(2) + "FYWVKJXQZ";

    // ときどき、ミノ内の一直線に基本的な単語を仕込む。完成形を固定しすぎず、偶然そろう楽しさも残す。
    private static final double WORD_FRIENDLY_CHANCE = 0.7;
    private static final double FOUR_LETTER_CHANCE = 0.35;

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

    private PieceGenerator() {
    }

    public static Piece next(Rng rng) {
        Shape shape = SHAPES[rng.nextInt(SHAPES.length)];
        int[][] cells = shape.cells();
        char[] letters = new char[cells.length];
        for (int i = 0; i < letters.length; i++) {
            letters[i] = LETTER_POOL.charAt(rng.nextInt(LETTER_POOL.length()));
        }

        List<int[][]> lines = shape.wordLines();
        if (!lines.isEmpty() && rng.next() < WORD_FRIENDLY_CHANCE) {
            boolean fourLetters = shape == Shape.I && rng.next() < FOUR_LETTER_CHANCE;
            List<String> words = fourLetters ? COMMON_FOUR_LETTER_WORDS : COMMON_THREE_LETTER_WORDS;
            String word = words.get(rng.nextInt(words.size()));
            int[][] line = fourLetters ? Shape.FOUR_LETTER_LINE : lines.get(rng.nextInt(lines.size()));
            for (int i = 0; i < line.length; i++) {
                letters[indexOf(cells, line[i])] = word.charAt(i);
            }
        }
        return new Piece(shape, new String(letters));
    }

    private static int indexOf(int[][] cells, int[] target) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i][0] == target[0] && cells[i][1] == target[1]) return i;
        }
        throw new IllegalStateException("Word line is outside the piece");
    }
}

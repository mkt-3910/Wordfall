package com.example.wordfall.game;

/** 状態を int 1つで保存・復元できる乱数生成器(mulberry32)。ゲームの途中状態をDBに保存するために使う。 */
public final class Rng {

    private int state;

    public Rng(int state) {
        this.state = state;
    }

    public int state() {
        return state;
    }

    /** 0以上1未満の値を返す。 */
    public double next() {
        state += 0x6D2B79F5;
        int t = state;
        t = (t ^ (t >>> 15)) * (t | 1);
        t ^= t + (t ^ (t >>> 7)) * (t | 61);
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
    }

    /** 0以上bound未満の整数を返す。 */
    public int nextInt(int bound) {
        return (int) (next() * bound);
    }
}

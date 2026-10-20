package com.example.wordfall.controller;

import com.example.wordfall.exception.BadRequestException;

/** ページ送りパラメータの共通チェック。 */
final class Paging {

    private Paging() {
    }

    static void validate(int page, int size, int maxSize) {
        if (page < 0 || size < 1 || size > maxSize) throw new BadRequestException("Invalid pagination");
    }
}

package com.example.wordfall.exception;

/** 対象が存在しない(または他のプレイヤーのもの)ことを表す。HTTPステータスへの変換は ApiExceptionHandler が行う。 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

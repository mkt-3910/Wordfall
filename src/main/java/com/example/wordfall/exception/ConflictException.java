package com.example.wordfall.exception;

/** 現在の状態と矛盾する操作ことを表す。HTTPステータスへの変換は ApiExceptionHandler が行う。 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

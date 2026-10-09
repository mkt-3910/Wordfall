package com.example.wordfall.exception;

/** リクエストの内容が不正ことを表す。HTTPステータスへの変換は ApiExceptionHandler が行う。 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

package com.example.wordfall.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.wordfall.exception.BadRequestException;
import com.example.wordfall.exception.ConflictException;
import com.example.wordfall.exception.NotFoundException;
import com.example.wordfall.game.GameEngine.IllegalMoveException;

/** サービス層の例外をHTTPステータスに変換する。内部の詳細はレスポンスに含めない。 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({BadRequestException.class, IllegalMoveException.class})
    public ProblemDetail badRequest(RuntimeException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
    }
}

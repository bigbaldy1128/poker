package com.bigbaldy.poker.exception.handler;

import com.bigbaldy.poker.exception.AbstractUncheckedException;
import com.bigbaldy.poker.exception.BaseErrorInfo;
import com.bigbaldy.poker.resource.ResponseResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseResource<?> runtimeExceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        return ResponseResource.builder()
                .code(BaseErrorInfo.FAILURE.getCode())
                .build();
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseResource<?> businessExceptionHandler(HttpClientErrorException e) {
        return ResponseResource.builder()
                .code(BaseErrorInfo.HTTP_CLIENT_ERROR.getCode())
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(AbstractUncheckedException.class)
    public ResponseResource<?> businessExceptionHandler(AbstractUncheckedException e) {
        return ResponseResource.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .build();
    }
}

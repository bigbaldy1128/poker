package com.bigbaldy.poker.exception.handler;

import com.bigbaldy.poker.exception.AbstractUncheckedException;
import com.bigbaldy.poker.exception.BaseErrorInfo;
import com.bigbaldy.poker.resource.ResponseResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ResponseResource<?>> runtimeExceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        ResponseResource<?> responseResource = ResponseResource.builder()
                .code(BaseErrorInfo.FAILURE.getCode())
                .build();
        return new ResponseEntity<>(responseResource, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ResponseResource<?>> businessExceptionHandler(HttpClientErrorException e) {
        ResponseResource<?> responseResource = ResponseResource.builder()
                .code(BaseErrorInfo.HTTP_CLIENT_ERROR.getCode())
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(responseResource, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AbstractUncheckedException.class)
    public ResponseEntity<ResponseResource<?>> businessExceptionHandler(AbstractUncheckedException e) {
        ResponseResource<?> responseResource = ResponseResource.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(responseResource, HttpStatus.OK);
    }
}

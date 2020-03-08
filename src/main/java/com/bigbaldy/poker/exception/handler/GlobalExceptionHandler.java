package com.bigbaldy.poker.exception.handler;

import com.bigbaldy.poker.exception.AbstractUncheckedException;
import com.bigbaldy.poker.exception.BaseErrorInfo;
import com.bigbaldy.poker.web.response.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResultVO runtimeExceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        return ResultVO.builder()
                .code(BaseErrorInfo.FAILURE.getCode())
                .build();
    }

    @ExceptionHandler(AbstractUncheckedException.class)
    public ResultVO businessExceptionHandler(AbstractUncheckedException e) {
        return ResultVO.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .build();
    }
}

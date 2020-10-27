package com.bigbaldy.poker.exception;

import org.springframework.http.HttpStatus;

public class InvalidInputArgumentException extends AbstractUncheckedException {
    public InvalidInputArgumentException(IErrorInfo errorInfo) {
        super(errorInfo);
    }

    @Override
    public int getModuleCode() {
        return 301;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    public enum ArgumentErrorInfo implements IErrorInfo {
        ILLEGAL_ARGUMENT(1, null),
        ILLEGAL_REQUEST(2, null),
        ;

        private int code;
        private String message;

        ArgumentErrorInfo(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}

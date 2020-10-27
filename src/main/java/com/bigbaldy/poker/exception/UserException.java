package com.bigbaldy.poker.exception;

import org.springframework.http.HttpStatus;

public class UserException extends AbstractUncheckedException {
    public UserException(IErrorInfo errorInfo) {
        super(errorInfo);
    }

    @Override
    public int getModuleCode() {
        return 201;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.OK;
    }

    public enum UserErrorInfo implements IErrorInfo{
        NOT_EXISTS(1),
        ;

        private int code;

        UserErrorInfo(int code) {
            this.code = code;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return null;
        }
    }
}

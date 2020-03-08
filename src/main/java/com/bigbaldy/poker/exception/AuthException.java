package com.bigbaldy.poker.exception;

//103xxx
public class AuthException extends AbstractUncheckedException {
    public AuthException(IErrorInfo errorInfo) {
        super(errorInfo);
    }

    @Override
    public int getModuleCode() {
        return 3;
    }

    public enum AuthErrorInfo implements IErrorInfo {
        NO_AUTH_HEADER(1, "no authorization header"),
        AUTH_FAILURE(2, "authorization failure"),
        ;

        private int code;
        private String message;

        AuthErrorInfo(int code, String message) {
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

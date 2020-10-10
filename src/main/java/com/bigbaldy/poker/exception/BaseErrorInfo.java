package com.bigbaldy.poker.exception;

public enum BaseErrorInfo implements IErrorInfo {
    SUCCESS(0),
    FAILURE(1),
    HTTP_ERROR(2),
    ;

    private int code;
    private String message;

    BaseErrorInfo(int code){
        this.code = code;
    }

    BaseErrorInfo(int code, String message) {
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

package com.bigbaldy.poker.web.response;

import com.bigbaldy.poker.exception.IErrorInfo;

public class ResultVOBuilder<T> {
    int code;
    String message;
    T data;

    public ResultVOBuilder<T> code(int code) {
        this.code = code;
        return this;
    }

    public ResultVOBuilder<T> message(String message) {
        this.message = message;
        return this;
    }

    public ResultVOBuilder<T> data(T data) {
        this.data = data;
        return this;
    }

    public ResultVOBuilder<T> errorInfo(IErrorInfo errorInfo) {
        this.code = errorInfo.getCode();
        this.message = errorInfo.getMessage();
        return this;
    }

    public ResultVO<T> build() {
        return new ResultVO<>(this.code, this.message, this.data);
    }
}

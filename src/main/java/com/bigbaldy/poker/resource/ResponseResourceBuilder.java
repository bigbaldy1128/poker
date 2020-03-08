package com.bigbaldy.poker.resource;

import com.bigbaldy.poker.exception.IErrorInfo;

public class ResponseResourceBuilder<T> {
    int code;
    String message;
    T data;

    public ResponseResourceBuilder<T> code(int code) {
        this.code = code;
        return this;
    }

    public ResponseResourceBuilder<T> message(String message) {
        this.message = message;
        return this;
    }

    public ResponseResourceBuilder<T> data(T data) {
        this.data = data;
        return this;
    }

    public ResponseResourceBuilder<T> errorInfo(IErrorInfo errorInfo) {
        this.code = errorInfo.getCode();
        this.message = errorInfo.getMessage();
        return this;
    }

    public ResponseResource<T> build() {
        return new ResponseResource<>(this.code, this.message, this.data);
    }
}

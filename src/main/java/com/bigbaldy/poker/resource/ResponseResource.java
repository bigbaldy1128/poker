package com.bigbaldy.poker.resource;

import com.bigbaldy.poker.exception.BaseErrorInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class ResponseResource<T> implements Serializable {
    private static final long serialVersionUID = 8679057964543952355L;

    public static final ResponseResource<?> SUCCESS = ResponseResource.builder().errorInfo(BaseErrorInfo.SUCCESS).build();
    public static final ResponseResource<?> FAILURE = ResponseResource.builder().errorInfo(BaseErrorInfo.FAILURE).build();

    @Getter
    private int code;
    @Getter @Setter
    private String message;
    @Getter @Setter
    private T data;

    public ResponseResource() {
    }

    ResponseResource(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseResourceBuilder<T> builder() {
        return new ResponseResourceBuilder<>();
    }

    public static <T> ResponseResource<T> create(T data) {
        return ResponseResource.<T>builder().errorInfo(BaseErrorInfo.SUCCESS).data(data).build();
    }
}

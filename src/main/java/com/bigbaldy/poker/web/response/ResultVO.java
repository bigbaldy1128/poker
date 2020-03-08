package com.bigbaldy.poker.web.response;

import com.bigbaldy.poker.exception.BaseErrorInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class ResultVO<T> implements Serializable {
    private static final long serialVersionUID = 8679057964543952355L;

    public static final ResultVO<?> SUCCESS = ResultVO.builder().errorInfo(BaseErrorInfo.SUCCESS).build();
    public static final ResultVO<?> FAILURE = ResultVO.builder().errorInfo(BaseErrorInfo.FAILURE).build();

    @Getter
    private int code;
    @Getter @Setter
    private String message;
    @Getter @Setter
    private T data;

    public ResultVO() {
    }

    ResultVO(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultVOBuilder<T> builder() {
        return new ResultVOBuilder<>();
    }

    public static <T> ResultVO<T> create(T data) {
        return ResultVO.<T>builder().errorInfo(BaseErrorInfo.SUCCESS).data(data).build();
    }
}

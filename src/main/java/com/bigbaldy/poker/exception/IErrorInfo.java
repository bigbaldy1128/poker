package com.bigbaldy.poker.exception;

import java.io.Serializable;

public interface IErrorInfo extends Serializable {
    int getCode();
    String getMessage();
}

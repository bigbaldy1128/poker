package com.bigbaldy.poker.util;

import com.bigbaldy.poker.exception.AbstractUncheckedException;
import com.bigbaldy.poker.resource.ResponseResource;

/**
 * @author wangjinzhao on 2020/10/27
 */
public class ExceptionUtil {
    public static ResponseResource<?> toResponseResource(AbstractUncheckedException e){
        return ResponseResource.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .build();
    }

}

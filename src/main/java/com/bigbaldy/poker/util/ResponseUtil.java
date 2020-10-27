package com.bigbaldy.poker.util;

import com.bigbaldy.poker.exception.AbstractUncheckedException;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author wangjinzhao on 2020/10/27
 */
public class ResponseUtil {
    public static void writeExceptionToResponse(HttpServletResponse httpServletResponse, AbstractUncheckedException exception) throws IOException {
        httpServletResponse.setStatus(exception.getHttpStatus().value());
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json; charset=utf-8");
        try (PrintWriter writer = httpServletResponse.getWriter()) {
            JsonUtil.getDefaultObjectMapper().writeValue(writer, exception.toResponseResource());
        }
    }
}

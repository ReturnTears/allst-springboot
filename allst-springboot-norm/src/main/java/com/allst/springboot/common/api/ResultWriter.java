package com.allst.springboot.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 过滤器里还没有进入 ControllerAdvice 时，用它直接写出统一返回体。
 */
public final class ResultWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResultWriter() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, Result<?> result) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(result));
    }
}

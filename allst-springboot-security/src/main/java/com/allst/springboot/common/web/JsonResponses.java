package com.allst.springboot.common.web;

import com.allst.springboot.common.api.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 过滤器里没有控制器返回值，用这个方法直接写统一 JSON。
 */
public final class JsonResponses {

    private JsonResponses() {
    }

    public static void write(HttpServletResponse response, ObjectMapper mapper, int httpStatus, ApiResult<?> body)
            throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getWriter(), body);
    }
}

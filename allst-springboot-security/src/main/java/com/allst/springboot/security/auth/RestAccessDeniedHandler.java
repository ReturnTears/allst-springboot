package com.allst.springboot.security.auth;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.JsonResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 已经登录但权限不够。例如 alice 访问 {@code GET /api/audit/logins}，过滤链匹配到 audit:read 失败后进这里，返回 403。
 * 和 401 分开：401 表示「你是谁还不知道」，403 表示「我知道你是谁，但这件事不让你做」。
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    public RestAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        JsonResponses.write(response, mapper, 403, ApiResult.fail(ResultCode.FORBIDDEN, "没有权限"));
    }
}

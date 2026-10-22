package com.allst.springboot.security.auth;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.JsonResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未登录时返回 JSON，不跳转 HTML 登录页。basic 为 true 时附带 WWW-Authenticate，方便 HTTP Basic 客户端。
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;
    private final boolean basic;

    public RestAuthenticationEntryPoint(ObjectMapper mapper, boolean basic) {
        this.mapper = mapper;
        this.basic = basic;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        if (basic) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"allst\"");
        }
        JsonResponses.write(response, mapper, 401, ApiResult.fail(ResultCode.UNAUTHORIZED, "未登录或登录已失效"));
    }
}

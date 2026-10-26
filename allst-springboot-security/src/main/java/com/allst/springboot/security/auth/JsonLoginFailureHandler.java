package com.allst.springboot.security.auth;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.JsonResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@code POST /session/login} 失败后的出口。锁定和停用给出不同业务码，其余失败都说「用户名或密码错误」。
 * 不区分用户不存在，是为了不让别人用这个接口试出哪些用户名是真的。
 */
public class JsonLoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper mapper;

    public JsonLoginFailureHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        int code = ResultCode.BAD_CREDENTIALS;
        String message = "用户名或密码错误";
        if (exception instanceof LockedException) {
            code = ResultCode.LOCKED;
            message = "账号已锁定，请稍后再试";
        } else if (exception instanceof DisabledException) {
            code = ResultCode.DISABLED;
            message = "账号已停用";
        }
        JsonResponses.write(response, mapper, 401, ApiResult.fail(code, message));
    }
}

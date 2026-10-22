package com.allst.springboot.security.auth;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.web.dto.UserView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.allst.springboot.common.web.JsonResponses;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 会话登录成功后写 JSON，而不是重定向到页面。见 docs/04-session-management.md。
 */
public class JsonLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper mapper;

    public JsonLoginSuccessHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        JsonResponses.write(response, mapper, 200, ApiResult.ok(UserView.from(user)));
    }
}

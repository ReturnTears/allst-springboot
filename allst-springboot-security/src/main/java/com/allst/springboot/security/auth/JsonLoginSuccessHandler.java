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
 * {@code POST /session/login} 成功后的出口。默认的成功处理器会 302 到某个页面，接口调用方拿不到 JSON，所以换成直接写用户信息。
 * 会话号已经由 {@code LoginSessionStrategy} 换过并写进 Cookie，这里只负责响应体。
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

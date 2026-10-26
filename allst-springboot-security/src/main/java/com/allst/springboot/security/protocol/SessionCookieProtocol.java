package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

/** 目录项：Cookie 会话。登录由过滤链里的表单过滤器处理，{@code SessionController} 只负责登录之后的查询。 */
@Component
public class SessionCookieProtocol implements SecurityProtocol {

    @Override
    public String id() {
        return "session-cookie";
    }

    @Override
    public String title() {
        return "会话 Cookie";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String description() {
        return "POST /session/login，表单字段 username、password，并携带 CSRF。";
    }
}

package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

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

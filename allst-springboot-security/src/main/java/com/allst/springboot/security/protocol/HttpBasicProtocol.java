package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

@Component
public class HttpBasicProtocol implements SecurityProtocol {

    @Override
    public String id() {
        return "http-basic";
    }

    @Override
    public String title() {
        return "HTTP Basic";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String description() {
        return "GET /basic/me，请求头 Authorization: Basic。每次请求都重新认证，不建立会话。";
    }
}

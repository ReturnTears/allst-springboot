package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

@Component
public class FormPasswordProtocol implements SecurityProtocol {

    @Override
    public String id() {
        return "form-password";
    }

    @Override
    public String title() {
        return "用户名口令";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String description() {
        return "POST /api/auth/login，JSON 用户名和口令，成功后签发 JWT。";
    }
}

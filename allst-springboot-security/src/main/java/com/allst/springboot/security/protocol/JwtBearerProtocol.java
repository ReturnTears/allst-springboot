package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

@Component
public class JwtBearerProtocol implements SecurityProtocol {

    @Override
    public String id() {
        return "jwt-bearer";
    }

    @Override
    public String title() {
        return "JWT Bearer";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String description() {
        return "Authorization: Bearer 访问令牌，由 JwtAuthenticationFilter 校验。";
    }
}

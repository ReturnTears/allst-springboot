package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

/** 目录项：访问令牌。校验发生在 {@code JwtAuthenticationFilter}，这条记录本身不参与验签。 */
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

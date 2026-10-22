package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

/**
 * SAML 只保留目录项。启用时再引入 spring-security-saml2-service-provider，并增加一条过滤链。
 */
@Component
public class Saml2Protocol implements SecurityProtocol {

    @Override
    public String id() {
        return "saml2";
    }

    @Override
    public String title() {
        return "SAML 2.0";
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public String description() {
        return "当前未接入。需要身份提供方元数据后再增加 RelyingPartyRegistration 和 SecurityFilterChain。";
    }
}

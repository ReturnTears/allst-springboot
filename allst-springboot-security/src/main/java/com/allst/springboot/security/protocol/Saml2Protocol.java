package com.allst.springboot.security.protocol;

import org.springframework.stereotype.Component;

/**
 * 目录里占一个位置，{@code enabled()} 固定是 false。当前工程没有 SAML 依赖，也不会有对应的过滤链。
 * 以后要接时再引入依赖、配置身份提供方，并单独加一条链；只改这个类不会打开登录。
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

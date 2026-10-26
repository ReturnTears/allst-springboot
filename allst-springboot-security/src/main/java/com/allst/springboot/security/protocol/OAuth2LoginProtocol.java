package com.allst.springboot.security.protocol;

import com.allst.springboot.config.SecurityProperties;
import org.springframework.stereotype.Component;

/** 目录项：授权码登录。过滤链是否存在取决于 {@code security.oauth2.enabled}，这个类只反映开关状态。 */
@Component
public class OAuth2LoginProtocol implements SecurityProtocol {

    private final SecurityProperties properties;

    public OAuth2LoginProtocol(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String id() {
        return "oauth2-login";
    }

    @Override
    public String title() {
        return "OAuth2 / OIDC 授权码";
    }

    @Override
    public boolean enabled() {
        return properties.getOauth2().isEnabled();
    }

    @Override
    public String description() {
        return "security.oauth2.enabled=true 时注册 /oauth2/** 与 /login/oauth2/**。身份提供方的用户名需与本地账号一致。";
    }
}

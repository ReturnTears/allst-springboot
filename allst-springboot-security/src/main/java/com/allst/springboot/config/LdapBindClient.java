package com.allst.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * 只在 {@code POST /api/auth/ldap} 里被调用。不注册成全局 {@code AuthenticationProvider}：
 * 如果挂进 {@code AuthenticationManager}，普通登录口令错误后框架还会再去目录上试一次，本地账号和目录账号会搅在一起。
 * 绑定只回答「目录里的口令对不对」，通过后仍用同名本地用户签发 JWT。
 */
@Component
@ConditionalOnProperty(prefix = "security.ldap", name = "enabled", havingValue = "true")
public class LdapBindClient {

    private final LdapAuthenticationProvider provider;

    public LdapBindClient(LdapContextSource contextSource) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[]{"uid={0},ou=people"});
        this.provider = new LdapAuthenticationProvider(authenticator);
    }

    public Authentication bind(Authentication authentication) {
        return provider.authenticate(authentication);
    }
}

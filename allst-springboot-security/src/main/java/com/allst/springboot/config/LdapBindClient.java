package com.allst.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * LDAP 绑定不注册成全局 AuthenticationProvider，避免口令登录失败后落到目录上再试一次。
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

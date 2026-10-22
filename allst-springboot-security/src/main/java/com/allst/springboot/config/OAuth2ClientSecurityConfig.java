package com.allst.springboot.config;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.JsonResponses;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.security.user.SecurityUserDetailsService;
import com.allst.springboot.web.dto.TokenView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 授权码登录。只有 security.oauth2.enabled=true 且配置了客户端注册信息时才应启用。
 * 见 docs/06-protocol-integration.md。
 */
@Configuration
@ConditionalOnProperty(prefix = "security.oauth2", name = "enabled", havingValue = "true")
public class OAuth2ClientSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain oauth2Chain(HttpSecurity http, AuthenticationSuccessHandler oauth2SuccessHandler)
            throws Exception {
        http.requestMatchers().antMatchers("/oauth2/**", "/login/oauth2/**");
        http.csrf().disable();
        http.authorizeRequests().anyRequest().authenticated();
        http.oauth2Login().successHandler(oauth2SuccessHandler);
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler(SecurityUserDetailsService users, JwtTokenService tokens,
                                                             ObjectMapper objectMapper) {
        return (request, response, authentication) -> writeTokens(response, authentication, users, tokens, objectMapper);
    }

    private static void writeTokens(javax.servlet.http.HttpServletResponse response, Authentication authentication,
                                    SecurityUserDetailsService users, JwtTokenService tokens, ObjectMapper objectMapper)
            throws java.io.IOException {
        try {
            SecurityUser user = (SecurityUser) users.loadUserByUsername(authentication.getName());
            TokenView view = tokens.issue(user);
            JsonResponses.write(response, objectMapper, 200, ApiResult.ok(view));
        } catch (UsernameNotFoundException ex) {
            JsonResponses.write(response, objectMapper, 401,
                    ApiResult.fail(ResultCode.EXTERNAL_NOT_BOUND, "外部身份已认证，但本地没有同名账号"));
        }
    }
}

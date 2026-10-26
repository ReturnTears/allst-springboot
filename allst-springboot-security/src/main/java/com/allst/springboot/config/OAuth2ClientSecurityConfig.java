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
 * 授权码登录，默认不加载。{@code security.oauth2.enabled=true} 时才注册这条链，避免没配客户端时启动失败。
 * <p>
 * 调用链：浏览器打开身份提供方 → 回调 {@code /login/oauth2/code/*} → 成功处理器按返回的用户名查本地账号 →
 * 本地有这个人就签发 JWT，没有就返回 40106。角色仍以本地库为准，不直接采用外部系统给的角色。
 * 这条链的 Order 是 0，比 {@code /api/**} 更靠前，所以回调地址不会被 JWT 链抢走。
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

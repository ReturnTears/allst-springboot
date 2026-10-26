package com.allst.springboot.security.jwt;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.JsonResponses;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.security.user.SecurityUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 每个进入 {@code /api/**} 的请求都会经过这里，但没有 Bearer 头时直接放行。
 * <p>
 * 有令牌时的顺序：验签和签发者 → 必须是 access 类型 → jti 不在注销名单 → 再按用户名读数据库。
 * 最后一步是故意的：令牌里虽然也写了角色，但权限以数据库为准，停用账号或改角色后下一次请求就会生效。
 * 校验通过后把 {@link SecurityUser} 放进 {@code SecurityContextHolder}，后面的 URL 规则和方法注解都从这里取当前用户。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokens;
    private final SecurityUserDetailsService users;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtTokenService tokens, SecurityUserDetailsService users, ObjectMapper mapper) {
        this.tokens = tokens;
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // 登录、刷新这些还没有令牌的请求会走到这里，后面的 permitAll 规则决定放不放行。
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = tokens.parse(tokens.stripBearer(header));
            if (!JwtTokenService.TYPE_ACCESS.equals(claims.get("typ", String.class))) {
                // 刷新令牌有效期更长，不能拿它直接当访问令牌用。
                reject(response, "请使用访问令牌");
                return;
            }
            if (tokens.accessRevoked(claims.getId())) {
                reject(response, "访问令牌已注销");
                return;
            }
            SecurityUser user = (SecurityUser) users.loadUserByUsername(claims.getSubject());
            if (!user.isEnabled() || !user.isAccountNonLocked() || !user.isAccountNonExpired()) {
                reject(response, "账号不可用");
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
            // 第三个参数是权限。credentials 传 null，避免把口令摘要再放进安全上下文。
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            reject(response, "令牌无效或已过期");
        }
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        JsonResponses.write(response, mapper, 401, ApiResult.fail(ResultCode.UNAUTHORIZED, message));
    }
}

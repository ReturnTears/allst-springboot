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
 * 读取 Authorization: Bearer，校验签名、类型、注销名单和账号状态，再写入 SecurityContext。
 * 权限每次从数据库展开，所以改角色后下一次请求就生效，不必等访问令牌过期。见 docs/09-jwt.md。
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
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = tokens.parse(tokens.stripBearer(header));
            if (!JwtTokenService.TYPE_ACCESS.equals(claims.get("typ", String.class))) {
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

package com.allst.springboot.security.jwt;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.JsonResponses;
import com.allst.springboot.security.user.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 密码过期后的第二道门，挂在 JWT 过滤器后面。
 * 用户还能登录，是为了让他调用改密接口；其余业务接口在这里被挡下，返回 40310。
 * 放行名单只有查看自己、退出、修改密码和查看口令规则。
 */
public class PasswordExpiryFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final String[] ALLOWED = {
            "/api/auth/me",
            "/api/auth/logout",
            "/api/password/change",
            "/api/password/policy"
    };

    private final ObjectMapper mapper;

    public PasswordExpiryFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            SecurityUser user = (SecurityUser) authentication.getPrincipal();
            if (user.isPasswordExpired() && !allowed(request)) {
                JsonResponses.write(response, mapper, 403,
                        ApiResult.fail(ResultCode.PASSWORD_EXPIRED, "密码已过期，请先修改密码"));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean allowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : ALLOWED) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}

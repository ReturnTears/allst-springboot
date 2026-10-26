package com.allst.springboot.security.auth;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.ClientIpResolver;
import com.allst.springboot.common.web.JsonResponses;
import com.allst.springboot.config.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 挡在四个登录地址最前面的 Servlet 过滤器，比 Security 过滤链更早。
 * 先问每一个 {@link LoginGuard}（现在只有维护开关），再按「IP + 路径」做一分钟滑动窗口。
 * 不读取 body：表单登录的用户名口令还要留给后面的认证过滤器。
 */
public class LoginProtectionFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;
    private final ObjectMapper mapper;
    private final List<LoginGuard> guards;
    private final SlidingWindowLimiter limiter = new SlidingWindowLimiter();

    public LoginProtectionFilter(SecurityProperties properties, ObjectMapper mapper, List<LoginGuard> guards) {
        this.properties = properties;
        this.mapper = mapper;
        this.guards = guards;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        for (LoginGuard guard : guards) {
            Optional<String> reject = guard.reject(request);
            if (reject.isPresent()) {
                JsonResponses.write(response, mapper, 503, ApiResult.fail(ResultCode.MAINTENANCE, reject.get()));
                return;
            }
        }
        String ip = ClientIpResolver.resolve(request, properties.getLogin().isTrustForwarded());
        String key = ip + " " + request.getRequestURI();
        int limit = properties.getLogin().getIpLimitPerMinute();
        if (!limiter.allow(key, limit, Duration.ofMinutes(1))) {
            JsonResponses.write(response, mapper, 429, ApiResult.fail(ResultCode.RATE_LIMIT, "登录尝试过于频繁"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}

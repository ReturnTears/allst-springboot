package com.allst.springboot.redis.ratelimit;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.JsonCodec;
import com.allst.springboot.redis.common.RateLimit;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final JsonCodec jsonCodec;

    public RateLimitInterceptor(RateLimitService rateLimitService, JsonCodec jsonCodec) {
        this.rateLimitService = rateLimitService;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        RateLimit limit = ((HandlerMethod) handler).getMethodAnnotation(RateLimit.class);
        if (limit == null) {
            return true;
        }
        String name = request.getRemoteAddr() + ":" + request.getRequestURI();
        Map<String, Object> decision = rateLimitService.fixedWindow(name, limit.limit(), limit.windowSeconds());
        if (Boolean.TRUE.equals(decision.get("allowed"))) {
            return true;
        }
        response.setStatus(429);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(jsonCodec.toJson(ApiResult.fail(429, "请求过于频繁，请稍后再试")));
        return false;
    }
}

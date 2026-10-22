package com.allst.springboot.aspect;

import com.allst.springboot.annotation.RateLimit;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.security.SlidingWindowLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 限流切面放在幂等切面外面，被限流的请求不会占用幂等键。
 * 对应规范：4 限流。
 */
@Aspect
@Component
@Order(10)
public class RateLimitAspect {

    private final SlidingWindowLimiter limiter;

    public RateLimitAspect(SlidingWindowLimiter limiter) {
        this.limiter = limiter;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String ip = RequestContext.get() == null ? request.getRemoteAddr() : RequestContext.get().getClientIp();
        String key = ip + "|" + request.getMethod() + "|" + request.getRequestURI();
        limiter.acquire(key, rateLimit.permits(), rateLimit.periodSeconds());
        return joinPoint.proceed();
    }
}

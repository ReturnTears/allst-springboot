package com.allst.springboot.structural.proxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring AOP 版本的代理：同一个「不改业务代码、横切加逻辑」的诉求，
 * 在容器里用切面表达，不需要手写 Proxy.newProxyInstance。
 *
 * 切点只对下单外观生效，避免把整个模块的调用都拦下来。
 */
@Aspect
@Component
public class ServiceTimingAspect {

    private final AtomicLong calls = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();

    @Around("execution(* com.allst.springboot.showcase.PlaceOrderFacade.*(..))")
    public Object time(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            totalNanos.addAndGet(System.nanoTime() - start);
            calls.incrementAndGet();
        }
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("facadeCallsThroughProxy", calls.get());
        stats.put("facadeTotalMillis", totalNanos.get() / 1_000_000);
        return stats;
    }
}

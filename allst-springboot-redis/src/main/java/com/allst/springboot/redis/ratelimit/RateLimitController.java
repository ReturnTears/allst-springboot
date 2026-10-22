package com.allst.springboot.redis.ratelimit;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.RateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratelimit")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/fixed")
    public ApiResult<?> fixed(@RequestParam(defaultValue = "demo") String name,
                              @RequestParam(defaultValue = "5") int limit,
                              @RequestParam(defaultValue = "10") int window) {
        return ApiResult.ok(rateLimitService.fixedWindow(name, limit, window));
    }

    @GetMapping("/slide")
    public ApiResult<?> slide(@RequestParam(defaultValue = "demo") String name,
                              @RequestParam(defaultValue = "5") int limit,
                              @RequestParam(defaultValue = "10") int window) {
        return ApiResult.ok(rateLimitService.slidingWindow(name, limit, window));
    }

    @GetMapping("/token")
    public ApiResult<?> token(@RequestParam(defaultValue = "demo") String name,
                              @RequestParam(defaultValue = "5") int capacity,
                              @RequestParam(defaultValue = "1") double rate) {
        return ApiResult.ok(rateLimitService.tokenBucket(name, capacity, rate));
    }

    @RateLimit(limit = 5, windowSeconds = 10)
    @GetMapping("/protected")
    public ApiResult<?> guarded() {
        return ApiResult.ok("注解限流放行");
    }
}

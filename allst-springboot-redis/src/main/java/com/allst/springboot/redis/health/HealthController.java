package com.allst.springboot.redis.health;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.config.DemoCatalog;
import com.allst.springboot.redis.persistence.ProductMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final StringRedisTemplate redis;
    private final ProductMapper productMapper;

    public HealthController(StringRedisTemplate redis, ProductMapper productMapper) {
        this.redis = redis;
        this.productMapper = productMapper;
    }

    @GetMapping("/catalog")
    public ApiResult<?> catalog() {
        return ApiResult.ok(DemoCatalog.items());
    }

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("time", OffsetDateTime.now().toString());
        try {
            String pong = redis.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> connection.ping());
            body.put("redis", pong);
        } catch (Exception ex) {
            body.put("redis", "DOWN");
            body.put("redisError", ex.getMessage());
        }
        try {
            body.put("products", productMapper.count());
            body.put("database", "UP");
        } catch (Exception ex) {
            body.put("database", "DOWN");
            body.put("databaseError", ex.getMessage());
        }
        return ApiResult.ok(body);
    }
}

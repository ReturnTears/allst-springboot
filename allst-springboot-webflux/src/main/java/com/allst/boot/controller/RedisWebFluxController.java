package com.allst.boot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author Hutu
 * @since 2024-08-14 下午 11:14
 */
@RestController
@RequestMapping(value = "/redis")
public class RedisWebFluxController {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @GetMapping(value = "/get/{key}")
    public Flux<String> findBy(@PathVariable("key") String key) {
        Object object = redisTemplate.opsForValue().get(key);
        if (object == null) {
            redisTemplate.opsForValue().set(key, "default value");
            return Flux.just("default value");
        }
        return Flux.just(object.toString());
    }

}

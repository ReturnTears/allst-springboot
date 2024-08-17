package com.allst.boot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author Hutu
 * @since 2024-08-14 下午 11:14
 */
@RestController
@RequestMapping(value = "/redis")
public class RedisWebFluxController {
    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @GetMapping(value = "/get/{key}")
    public Mono<String> findBy(@PathVariable("key") String key) {
        Mono<String> mono = reactiveRedisTemplate.opsForValue().get(key);
        System.out.println("come in method findBy");
        return mono;
    }

}

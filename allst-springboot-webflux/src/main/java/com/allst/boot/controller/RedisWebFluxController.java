package com.allst.boot.controller;

import com.allst.boot.domain.Book;
import com.allst.boot.repository.BookRepository;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private BookRepository bookRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @GetMapping(value = "/get/{key}")
    public Mono<String> findBy(@PathVariable("key") String key) {
        Mono<String> mono = reactiveRedisTemplate.opsForValue().get(key);
        System.out.println("come in method findBy");
        return mono;
    }

    @GetMapping(value = "/set/cache/{key}")
    public Mono<String> setCacheBy(@PathVariable("key") String key) {
        Book book = bookRepository.findBookByName(key);
        stringRedisTemplate.opsForValue().set(key, new Gson().toJson(book));
        return Mono.just("Cache Success !");
    }

    @GetMapping(value = "/get/cache/{key}")
    public Mono<String> getCacheBy(@PathVariable("key") String key) {
        ReactiveValueOperations<String, String> value = reactiveRedisTemplate.opsForValue();
        return value.get(key);
    }
}

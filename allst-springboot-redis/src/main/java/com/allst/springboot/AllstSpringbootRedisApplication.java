package com.allst.springboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.allst.springboot.redis.persistence")
@EnableScheduling
@EnableCaching
public class AllstSpringbootRedisApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllstSpringbootRedisApplication.class, args);
    }
}

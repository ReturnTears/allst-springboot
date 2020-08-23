package com.allst.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ServletComponentScan
@EnableCaching  // 开启基于注解的缓存
public class AllstSpringbootAdvsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllstSpringbootAdvsApplication.class, args);
    }

}

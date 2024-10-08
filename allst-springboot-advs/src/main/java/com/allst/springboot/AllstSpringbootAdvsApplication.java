package com.allst.springboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ServletComponentScan
@EnableCaching  // 开启基于注解的缓存
@EnableAsync    // 开启异步注解
@EnableScheduling   // 开启基于注解的定时任务
@MapperScan("com.allst.springboot.mapper")
public class AllstSpringbootAdvsApplication {

    public static void main(String[] args) {
        System.out.println("AllstSpringbootAdvsApplication.main");
        SpringApplication.run(AllstSpringbootAdvsApplication.class, args);
    }

}

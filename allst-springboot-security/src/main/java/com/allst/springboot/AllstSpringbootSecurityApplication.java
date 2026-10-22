package com.allst.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 安全规范示例。九项实现的说明、类名和调用方式见 {@code docs/README.md}。
 */
@SpringBootApplication
@EnableScheduling
public class AllstSpringbootSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllstSpringbootSecurityApplication.class, args);
    }
}

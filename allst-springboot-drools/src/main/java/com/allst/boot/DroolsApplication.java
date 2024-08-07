package com.allst.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Hutu
 * @since 2024-08-07 下午 09:37
 */
@SpringBootApplication
public class DroolsApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        SpringApplication.run(DroolsApplication.class, args);
    }
}
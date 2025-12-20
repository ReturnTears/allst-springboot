package com.allst.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Hutu
 * @since 2025-12-20 下午 07:35
 */
@SpringBootApplication
public class SatokenApplication {
    public static void main(String[] args) {
        SpringApplication.run(SatokenApplication.class, args);
        System.out.println("Hello Sa-token!");
    }
}

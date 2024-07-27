package com.allst.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Hutu
 * @since 2024-07-27 上午 08:58
 */
@SpringBootApplication
public class DocApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        SpringApplication.run(DocApplication.class, args);
    }
}
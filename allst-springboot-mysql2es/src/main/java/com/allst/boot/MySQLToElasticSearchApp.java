package com.allst.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Hutu
 * @since 2024-08-09 下午 09:29
 */
@SpringBootApplication
public class MySQLToElasticSearchApp {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        SpringApplication.run(MySQLToElasticSearchApp.class, args);
    }
}
package com.allst.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Hutu
 * @since 2024-07-28 下午 04:50
 */
@SpringBootApplication
public class DockerApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        SpringApplication.run(DockerApplication.class, args);
    }
}
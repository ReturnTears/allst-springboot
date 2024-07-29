package com.allst.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Hutu
 * @since 2024-07-29 下午 08:31
 */
@SpringBootApplication
public class GraphqlApplication {
    public static void main(String[] args) {
        System.out.println("Hello graphql!");
        SpringApplication.run(GraphqlApplication.class, args);
    }
}
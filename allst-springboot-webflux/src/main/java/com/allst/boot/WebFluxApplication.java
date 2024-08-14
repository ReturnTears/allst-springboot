package com.allst.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Hutu
 * @since 2024-08-14 下午 10:27
 */
@SpringBootApplication
public class WebFluxApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        ConfigurableApplicationContext context = SpringApplication.run(WebFluxApplication.class, args);
        boolean running = context.isRunning();
        System.out.println("SpringBoot应用是否正在运行：" + running);
    }
}
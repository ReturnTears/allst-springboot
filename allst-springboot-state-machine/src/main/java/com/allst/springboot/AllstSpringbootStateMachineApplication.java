package com.allst.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AllstSpringbootStateMachineApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllstSpringbootStateMachineApplication.class, args);
    }
}

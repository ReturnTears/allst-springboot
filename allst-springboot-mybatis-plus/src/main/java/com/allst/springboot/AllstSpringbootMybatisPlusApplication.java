package com.allst.springboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.allst.springboot")
public class AllstSpringbootMybatisPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllstSpringbootMybatisPlusApplication.class, args);
    }

}

package com.allst.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * Hello world!
 *
 * EnableMBeanExport 解决JMX重复注册Bean
 *
 */
@SpringBootApplication
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
public class App {
    public static void main( String[] args ) {
        System.out.println( "Hello World!" );
        SpringApplication.run(App.class, args);
    }
}

package com.allst.springboot.config;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author Hutu
 * @since 2023-05-22 下午 07:46
 */
@Configuration
public class ApplicationStartConfig implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        System.out.println("----ApplicationStartConfig----");
    }
}

package com.allst.springboot.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author Hutu
 * @since 2023-06-15 上午 10:47
 */
@Configuration
public class ApplicationJobListener implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        System.out.println("ApplicationJobListener started");
    }
}

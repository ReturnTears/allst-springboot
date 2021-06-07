package com.allst.springboot.config;

import org.springframework.boot.web.servlet.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

/**
 * @author June
 * @since 2021年06月
 */
@Configuration
public class MyWebAutoConfiguration {
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        // 这里可以自定义自己想定义的一个类
        return new OrderedHiddenHttpMethodFilter();
    }
}

package com.allst.exercise.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfigurer 是 Spring MVC 提供的一个接口，用于定制 Spring MVC 的行为。
 * 在 Spring Boot 中，这个接口非常有用，因为它允许你在不编写额外配置类的情况下，对 Spring MVC 的各种功能进行扩展和配置。
 *
 * @author Hutu
 * @since 2024-07-27 下午 11:49
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new CustomersFormatter());
    }
}

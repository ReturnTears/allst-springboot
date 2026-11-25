package com.allst.springboot;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Spring Boot还提供了SpringApplicationBuilder工具类，通过该工具类能以流式API创建SpringApplication，并启动Spring Boot应用
 */
// @SpringBootApplication
public class BasicStreamApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                // 加载父容器对于的配置类
                // .sources(BasicStreamApplication.class)
                // 当前类的对于配置作为子容器
        .child(BasicStreamApplication.class)
        .lazyInitialization(true)
        .bannerMode(Banner.Mode.OFF)
        .run(args);
    }

}

package com.allst.springboot;

import com.allst.springboot.config.AppConfig;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ImportResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Spring Boot还提供了SpringApplicationBuilder工具类，通过该工具类能以流式API创建SpringApplication，并启动Spring Boot应用
 */
// @SpringBootApplication
public class AllstSpringbootBasicStramApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                // 加载父容器对于的配置类
                // .sources(AllstSpringbootParentApplication.class)
                // 当前类的对于配置作为子容器
        .child(AllstSpringbootBasicStramApplication.class)
        .lazyInitialization(true)
        .bannerMode(Banner.Mode.OFF)
        .run(args);
    }

}

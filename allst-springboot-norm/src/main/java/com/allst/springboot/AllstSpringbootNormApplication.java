package com.allst.springboot;

import com.allst.springboot.config.NormProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 规范示例应用。各规范的说明见项目 docs 目录，请求链路见 docs/README.md。
 */
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(NormProperties.class)
public class AllstSpringbootNormApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllstSpringbootNormApplication.class, args);
    }
}

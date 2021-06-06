package com.allst.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 * @author June
 * @since 2021年06月
 */
@SpringBootApplication
public class LogApplication {

    // 声明日志记录器
    final static Logger logger = LoggerFactory.getLogger(LogApplication.class);

    public static void main(String ...args) {
        SpringApplication.run(LogApplication.class, args);

        logger.trace("trace...");
        logger.info("info...");
        logger.debug("debug...");
        logger.warn("warn...");
        logger.error("error...");
    }
}

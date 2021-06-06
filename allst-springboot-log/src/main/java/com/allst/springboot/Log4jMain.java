package com.allst.springboot;

import org.apache.log4j.Logger;

/**
 * 采用开源Log4j日志记录： log4j（实现） + slf4j（门面） + 桥接器
 * No appenders could be found for logger (com.allst.springboot.Log4jMain).
 * 需要使用日志配置文件
 * log4j.properties
 */
public class Log4jMain {
    public static void main(String ...args ) {
        Logger logger = Logger.getLogger(Log4jMain.class);
        logger.info("logger log4j ");
    }
}

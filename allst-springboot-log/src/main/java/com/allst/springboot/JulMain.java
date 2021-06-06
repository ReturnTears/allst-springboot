package com.allst.springboot;

import java.util.logging.Logger;

/**
 * 采用官方Jul日志记录: Jul（实现） + Jcl（门面）
 * 不加入任何依赖
 */
public class JulMain {
    public static void main(String ...args ) {
        Logger logger = Logger.getLogger(JulMain.class.getName());
        logger.info("logger jul ");
    }
}

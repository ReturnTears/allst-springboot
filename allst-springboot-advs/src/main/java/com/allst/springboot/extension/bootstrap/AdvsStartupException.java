package com.allst.springboot.extension.bootstrap;

/**
 * 扩展点主动中断启动时使用的异常。
 * FailureAnalyzer 只识别这一类型，不会接管数据源或 Redis 的失败分析。
 */
public class AdvsStartupException extends RuntimeException {

    public AdvsStartupException(String message) {
        super(message);
    }
}

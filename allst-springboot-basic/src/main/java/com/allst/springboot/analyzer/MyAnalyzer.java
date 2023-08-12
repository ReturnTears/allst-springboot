package com.allst.springboot.analyzer;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.net.BindException;

/**
 * 自定义失败分析器
 *
 * @author Hutu
 * @since 2023-08-12 下午 04:52
 */
public class MyAnalyzer extends AbstractFailureAnalyzer<BindException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, BindException cause) {
        cause.printStackTrace();
        return new FailureAnalysis(String.format("程序启动出错，程序绑定的端口被占用:%s", cause.getMessage()),
                String.format("请先停止占用%s端口的程序后再运行本程序或使用server.port改变本应用的端口", 8080), cause);
    }
}

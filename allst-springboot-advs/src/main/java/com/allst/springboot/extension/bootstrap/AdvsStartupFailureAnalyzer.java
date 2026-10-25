package com.allst.springboot.extension.bootstrap;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * 只解释本模块抛出的启动异常，不拦截数据源、Redis 或其他组件的失败报告。
 */
public class AdvsStartupFailureAnalyzer extends AbstractFailureAnalyzer<AdvsStartupException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, AdvsStartupException cause) {
        StartupTraceLog.record(StartupPhase.FAILURE_ANALYZER,
                "分析 AdvsStartupException: " + cause.getMessage());
        return new FailureAnalysis(
                "ALLST 启动扩展点中断了启动: " + cause.getMessage(),
                "若这是有意的配置检查，请在当前 profile 中设置 advs.extension.required-token；"
                        + "若希望正常启动，请保持 advs.extension.fail-fast=false。"
                        + "默认项由 AdvsEnvironmentPostProcessor 用最低优先级补齐，业务配置写在 application-beta.yml 即可覆盖。",
                cause);
    }
}

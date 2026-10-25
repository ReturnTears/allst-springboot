package com.allst.springboot.extension.bootstrap;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 包住整次 SpringApplication.run 的外层监听器。
 * <p>
 * Boot 2.3 通过 spring.factories 反射创建，构造器必须是 (SpringApplication, String[])。
 * order 取最高优先级，使这些回调先于 EventPublishingRunListener 发布的对应事件。
 */
public class AdvsSpringApplicationRunListener implements SpringApplicationRunListener, Ordered {

    private final String[] args;
    private final long startNanos;

    public AdvsSpringApplicationRunListener(SpringApplication application, String[] args) {
        this.args = args == null ? new String[0] : args;
        this.startNanos = System.nanoTime();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void starting() {
        StartupTraceLog.record(StartupPhase.RUN_LISTENER_STARTING,
                "进入 SpringApplication.run，原始参数个数=" + args.length);
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        StartupTraceLog.record(StartupPhase.RUN_LISTENER_ENVIRONMENT_PREPARED,
                "Environment 已创建。配置文件多半尚未加载，此处不读取 server.port 或数据源。");
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        boolean clockReady = context.getBeanFactory().containsSingleton("advsStartupClock");
        StartupTraceLog.record(StartupPhase.RUN_LISTENER_CONTEXT_PREPARED,
                "初始化器已执行，advsStartupClock 已注册=" + clockReady
                        + "，上下文=" + context.getClass().getSimpleName());
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        StartupTraceLog.record(StartupPhase.RUN_LISTENER_CONTEXT_LOADED,
                "启动类已载入，Bean 定义数量=" + context.getBeanFactory().getBeanDefinitionCount()
                        + "，refresh 尚未开始。");
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        StartupTraceLog.record(StartupPhase.RUN_LISTENER_STARTED,
                "上下文已刷新，Runner 尚未执行，已耗时 " + elapsedMillis() + " ms。");
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        StartupTraceLog.record(StartupPhase.RUN_LISTENER_RUNNING,
                "Runner 已结束，run 即将返回，总耗时 " + elapsedMillis() + " ms。");
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        String contextState = context == null ? "context=null" : context.getClass().getSimpleName();
        StartupTraceLog.record(StartupPhase.RUN_LISTENER_FAILED,
                "启动失败，" + contextState + "，异常=" + exception.getClass().getName()
                        + "，消息=" + exception.getMessage());
    }

    private long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}

package com.allst.springboot.extension.bootstrap;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

/**
 * 容器创建前就能工作的事件监听器。
 * 只声明早期事件和失败事件，避免和容器内的监听器重复记录刷新、就绪和关闭。
 */
public class AdvsEarlyApplicationListener implements GenericApplicationListener {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> rawClass = eventType.resolve();
        if (rawClass == null) {
            return false;
        }
        return ApplicationStartingEvent.class.isAssignableFrom(rawClass)
                || ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(rawClass)
                || ApplicationContextInitializedEvent.class.isAssignableFrom(rawClass)
                || ApplicationPreparedEvent.class.isAssignableFrom(rawClass)
                || ApplicationFailedEvent.class.isAssignableFrom(rawClass);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationStartingEvent) {
            StartupTraceLog.record(StartupPhase.EVENT_STARTING, "ApplicationStartingEvent 已发布。");
        } else if (event instanceof ApplicationEnvironmentPreparedEvent) {
            StartupTraceLog.record(StartupPhase.EVENT_ENVIRONMENT_PREPARED,
                    "ApplicationEnvironmentPreparedEvent 已发布，随后才会执行 EnvironmentPostProcessor。");
        } else if (event instanceof ApplicationContextInitializedEvent) {
            ApplicationContextInitializedEvent initialized = (ApplicationContextInitializedEvent) event;
            StartupTraceLog.record(StartupPhase.EVENT_CONTEXT_INITIALIZED,
                    "上下文已初始化，类型=" + initialized.getApplicationContext().getClass().getSimpleName());
        } else if (event instanceof ApplicationPreparedEvent) {
            StartupTraceLog.record(StartupPhase.EVENT_PREPARED, "ApplicationPreparedEvent 已发布，即将 refresh。");
        } else if (event instanceof ApplicationFailedEvent) {
            ApplicationFailedEvent failed = (ApplicationFailedEvent) event;
            Throwable exception = failed.getException();
            StartupTraceLog.record(StartupPhase.EVENT_FAILED,
                    "ApplicationFailedEvent，异常=" + exception.getClass().getName()
                            + "，消息=" + exception.getMessage());
        }
    }
}

package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 容器刷新之后才能作为 Bean 收到的事件。
 */
@Component
public class AdvsContextEventListener implements ApplicationListener<ApplicationEvent> {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            StartupTraceLog.record(StartupPhase.EVENT_CONTEXT_REFRESHED,
                    "ContextRefreshedEvent，Runner 尚未开始。");
        } else if (event instanceof ApplicationStartedEvent) {
            StartupTraceLog.record(StartupPhase.EVENT_STARTED, "ApplicationStartedEvent 已发布。");
        } else if (event instanceof ApplicationReadyEvent) {
            ApplicationReadyEvent readyEvent = (ApplicationReadyEvent) event;
            StartupTraceLog.record(StartupPhase.EVENT_READY,
                    "ApplicationReadyEvent，可用性=" + availabilityText(readyEvent));
        } else if (event instanceof ContextClosedEvent) {
            StartupTraceLog.record(StartupPhase.EVENT_CONTEXT_CLOSED,
                    "ContextClosedEvent。Bean 仍可使用，随后才是 Lifecycle.stop 和销毁回调。");
        }
    }

    private String availabilityText(ApplicationReadyEvent readyEvent) {
        try {
            Map<String, ApplicationAvailability> beans = readyEvent.getApplicationContext()
                    .getBeansOfType(ApplicationAvailability.class);
            if (beans.isEmpty()) {
                return "容器中没有 ApplicationAvailability";
            }
            ApplicationAvailability availability = beans.values().iterator().next();
            return "liveness=" + availability.getLivenessState()
                    + ", readiness=" + availability.getReadinessState();
        } catch (RuntimeException ex) {
            return "读取失败 " + ex.getClass().getSimpleName();
        }
    }
}

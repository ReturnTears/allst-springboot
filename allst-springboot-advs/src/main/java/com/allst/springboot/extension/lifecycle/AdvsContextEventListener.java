package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
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
                    "ApplicationReadyEvent 发布时读到的可用性=" + availabilityText(readyEvent)
                            + "。就绪状态要等本事件的监听器返回后才会改为 ACCEPTING_TRAFFIC。");
        } else if (event instanceof AvailabilityChangeEvent) {
            Object state = ((AvailabilityChangeEvent<?>) event).getState();
            if (state instanceof LivenessState) {
                StartupTraceLog.record(StartupPhase.LIVENESS_CHANGED,
                        "存活状态变为 " + state + "。这发生在 ApplicationStartedEvent 监听器返回之后、Runner 之前。");
            } else if (state instanceof ReadinessState) {
                StartupTraceLog.record(StartupPhase.READINESS_CHANGED,
                        "就绪状态变为 " + state
                                + "。ACCEPTING_TRAFFIC 表示可以接流量；关闭前会先变回 REFUSING_TRAFFIC。");
            }
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

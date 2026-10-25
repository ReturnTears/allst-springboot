package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * phase 使用 0，早于使用很大 phase 的内嵌容器生命周期。
 * Boot 2.3 的 Tomcat 往往在 onRefresh 时已经启动，因此这里只标记模块状态。
 */
@Component
public class AdvsSmartLifecycle implements SmartLifecycle {

    private volatile boolean running;

    @Override
    public void start() {
        running = true;
        StartupTraceLog.record(StartupPhase.LIFECYCLE_START,
                "SmartLifecycle.start，phase=0。模块标记为运行中；是否对外接流量仍以 ApplicationReadyEvent 为准。");
    }

    @Override
    public void stop() {
        running = false;
        StartupTraceLog.record(StartupPhase.LIFECYCLE_STOP, "SmartLifecycle.stop，模块标记为已停止。");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }
}

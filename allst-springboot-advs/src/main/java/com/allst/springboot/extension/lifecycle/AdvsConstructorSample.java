package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.stereotype.Component;

/**
 * 由组件扫描创建，用来观察构造器推断。
 * 工厂方法创建的 Bean 不会经过 {@code determineCandidateConstructors}。
 */
@Component
public class AdvsConstructorSample {

    public AdvsConstructorSample() {
        StartupTraceLog.record(StartupPhase.CONSTRUCTOR,
                "AdvsConstructorSample 由组件扫描创建，构造器推断之后才会进入这里。");
    }
}

package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import com.allst.springboot.mapper.EmployeeMapper;
import com.allst.springboot.service.DepartmentService;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 全部单例创建完成后的集中自检。
 */
@Component
public class AdvsSingletonReadyCallback implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    public AdvsSingletonReadyCallback(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        StartupTraceLog.record(StartupPhase.SMART_INITIALIZING_SINGLETON,
                "全部非懒加载单例已创建。缓存切点 " + StartupTraceLog.cachePoints().size()
                        + " 个，轨迹 " + StartupTraceLog.size()
                        + " 条，DepartmentService=" + count(DepartmentService.class)
                        + "，EmployeeMapper=" + count(EmployeeMapper.class) + "。");
    }

    private int count(Class<?> type) {
        try {
            return applicationContext.getBeanNamesForType(type).length;
        } catch (RuntimeException ex) {
            return -1;
        }
    }
}

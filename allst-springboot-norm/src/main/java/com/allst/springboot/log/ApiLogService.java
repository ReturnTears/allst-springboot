package com.allst.springboot.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 日志落库放到业务线程池，请求线程只负责组好实体。
 */
@Slf4j
@Service
public class ApiLogService {

    private final ApiRequestLogRepository repository;

    public ApiLogService(ApiRequestLogRepository repository) {
        this.repository = repository;
    }

    @Async("normExecutor")
    public void save(ApiRequestLog entity) {
        try {
            repository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("访问日志落库失败: {}", ex.getMessage());
        }
    }
}

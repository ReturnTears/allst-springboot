package com.allst.springboot.domain.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 事务提交后的通知。这里用日志代替真实短信或消息队列，重点是主流程不在这里等待。
 * 对应规范：12 异步处理。
 */
@Slf4j
@Service
public class NotifyService {

    @Async("normExecutor")
    public void onUserCreated(Long userId, String username) {
        log.info("异步通知：用户已创建 userId={} username={}", userId, username);
    }

    @Async("normExecutor")
    public void onOrderCreated(Long orderId, String orderNo) {
        log.info("异步通知：订单已创建 orderId={} orderNo={}", orderId, orderNo);
    }
}

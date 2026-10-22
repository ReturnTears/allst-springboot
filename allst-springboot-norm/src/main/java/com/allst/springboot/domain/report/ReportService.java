package com.allst.springboot.domain.report;

import com.allst.springboot.domain.order.OrderRepository;
import com.allst.springboot.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 汇总统计放到业务线程池，控制器返回 CompletableFuture，释放容器线程。
 * 对应规范：12 异步处理。
 */
@Service
public class ReportService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final Executor normExecutor;

    public ReportService(UserRepository userRepository, OrderRepository orderRepository,
                         @Qualifier("normExecutor") Executor normExecutor) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.normExecutor = normExecutor;
    }

    public CompletableFuture<ReportVO> summary() {
        return CompletableFuture.supplyAsync(() -> {
            ReportVO vo = new ReportVO();
            vo.setUserCount(userRepository.count());
            vo.setOrderCount(orderRepository.count());
            vo.setGeneratedAt(LocalDateTime.now());
            return vo;
        }, normExecutor);
    }
}

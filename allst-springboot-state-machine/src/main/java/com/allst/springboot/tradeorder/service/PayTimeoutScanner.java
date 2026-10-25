package com.allst.springboot.tradeorder.service;

import com.allst.springboot.config.AppClock;
import com.allst.springboot.config.TradeProperties;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderEventCommand;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.domain.TradeOrder;
import com.allst.springboot.tradeorder.repo.TradeOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单走事件，不直接改状态。内存里的 timer 会随状态机实例一起停掉，所以用调度线程补发 TIMEOUT。
 */
@Component
public class PayTimeoutScanner {

    private static final Logger log = LoggerFactory.getLogger(PayTimeoutScanner.class);

    private final TradeProperties properties;
    private final AppClock clock;
    private final TradeOrderRepository orderRepository;
    private final TradeOrderAppService appService;

    public PayTimeoutScanner(TradeProperties properties,
                             AppClock clock,
                             TradeOrderRepository orderRepository,
                             TradeOrderAppService appService) {
        this.properties = properties;
        this.clock = clock;
        this.orderRepository = orderRepository;
        this.appService = appService;
    }

    @Scheduled(fixedDelayString = "${statemachine.trade.scan-delay-ms:60000}")
    public int scan() {
        LocalDateTime deadline = clock.now().minusMinutes(properties.getPayTimeoutMinutes());
        List<TradeOrder> expired = orderRepository.findByStatusInAndUpdatedAtBefore(
                List.of(OrderState.CREATED, OrderState.PAYING, OrderState.PAY_FAILED), deadline);
        int closed = 0;
        for (TradeOrder order : expired) {
            OrderEventCommand command = new OrderEventCommand();
            command.setEvent(OrderEvent.TIMEOUT);
            command.setOperator("system");
            command.setEventId("timeout-" + order.getOrderNo());
            command.setRemark("超时未支付，系统关闭");
            try {
                appService.fire(order.getOrderNo(), command);
                closed++;
            } catch (RuntimeException exception) {
                log.warn("close expired order {} failed: {}", order.getOrderNo(), exception.getMessage());
            }
        }
        return closed;
    }
}

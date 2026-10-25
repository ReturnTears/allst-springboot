package com.allst.springboot.tradeorder.service;

import com.allst.springboot.audit.TransitionLogRepository;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.lock.KeyStripedLock;
import com.allst.springboot.tradeorder.domain.OrderEventCommand;
import com.allst.springboot.tradeorder.domain.TradeOrder;
import com.allst.springboot.tradeorder.repo.TradeOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对控制器暴露的门面：创建、查询不串行；改变状态的事件先按订单号加锁，再进入事务。
 */
@Service
public class TradeOrderAppService {

    private final KeyStripedLock locks;
    private final TradeOrderFlowService flowService;
    private final TradeOrderRepository orderRepository;
    private final TransitionLogRepository logRepository;
    private final TradeOrderAssembler assembler;

    public TradeOrderAppService(KeyStripedLock locks,
                                TradeOrderFlowService flowService,
                                TradeOrderRepository orderRepository,
                                TransitionLogRepository logRepository,
                                TradeOrderAssembler assembler) {
        this.locks = locks;
        this.flowService = flowService;
        this.orderRepository = orderRepository;
        this.logRepository = logRepository;
        this.assembler = assembler;
    }

    public TradeOrderView create(String buyerName, String skuCode, int quantity, BigDecimal amount, int riskScore) {
        return flowService.create(buyerName, skuCode, quantity, amount, riskScore);
    }

    public TradeOrderView fire(String orderNo, OrderEventCommand command) {
        return locks.call("trade:" + orderNo, () -> flowService.fire(orderNo, command));
    }

    @Transactional(readOnly = true)
    public TradeOrderView get(String orderNo) {
        TradeOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        return assembler.toView(order, false);
    }

    @Transactional(readOnly = true)
    public List<TransitionLogView> logs(String orderNo) {
        if (!orderRepository.existsByOrderNo(orderNo)) {
            throw new BizException(404, "订单不存在");
        }
        return logRepository.findByMachineTypeAndMachineIdOrderByIdAsc(TradeOrderFlowService.MACHINE_TYPE, orderNo)
                .stream()
                .map(assembler::toLogView)
                .collect(Collectors.toList());
    }
}

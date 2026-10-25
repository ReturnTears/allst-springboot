package com.allst.springboot.tradeorder.service;

import com.allst.springboot.audit.TransitionLog;
import com.allst.springboot.audit.TransitionLogRepository;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.tradeorder.domain.OrderEventCatalog;
import com.allst.springboot.tradeorder.domain.OrderEventCommand;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.domain.TradeOrder;
import com.allst.springboot.tradeorder.machine.OrderMachineTemplate;
import com.allst.springboot.tradeorder.repo.SkuStockRepository;
import com.allst.springboot.tradeorder.repo.TradeOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TradeOrderFlowService {

    public static final String MACHINE_TYPE = "TRADE_ORDER";

    private final TradeOrderRepository orderRepository;
    private final SkuStockRepository stockRepository;
    private final TransitionLogRepository logRepository;
    private final OrderMachineTemplate machineTemplate;
    private final TradeOrderAssembler assembler;

    public TradeOrderFlowService(TradeOrderRepository orderRepository,
                                 SkuStockRepository stockRepository,
                                 TransitionLogRepository logRepository,
                                 OrderMachineTemplate machineTemplate,
                                 TradeOrderAssembler assembler) {
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
        this.logRepository = logRepository;
        this.machineTemplate = machineTemplate;
        this.assembler = assembler;
    }

    @Transactional
    public TradeOrderView create(String buyerName, String skuCode, int quantity,
                                 java.math.BigDecimal amount, int riskScore) {
        if (!stockRepository.existsBySkuCode(skuCode)) {
            throw new BizException("SKU 不存在: " + skuCode);
        }
        TradeOrder order = new TradeOrder();
        order.setOrderNo(nextOrderNo());
        order.setBuyerName(buyerName);
        order.setSkuCode(skuCode);
        order.setQuantity(quantity);
        order.setAmount(amount);
        order.setRiskScore(riskScore);
        order.setStatus(OrderState.CREATED);
        return assembler.toView(orderRepository.save(order), false);
    }

    @Transactional
    public TradeOrderView fire(String orderNo, OrderEventCommand command) {
        TradeOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (logRepository.existsByMachineTypeAndMachineIdAndEventId(MACHINE_TYPE, orderNo, command.getEventId())) {
            return assembler.toView(order, true);
        }
        OrderState before = order.getStatus();
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", order.getAmount());
        variables.put("riskScore", order.getRiskScore());
        variables.put("skuCode", order.getSkuCode());
        variables.put("quantity", order.getQuantity());
        variables.put("createdAt", order.getCreatedAt());
        Map<String, Object> headers = new HashMap<>();
        headers.put("operator", command.getOperator());
        headers.put("eventId", command.getEventId());
        headers.put("shipmentNo", command.getShipmentNo());
        headers.put("remark", command.getRemark());
        OrderMachineTemplate.FireResult result = machineTemplate.fire(
                orderNo, before, command.getEvent(), variables, headers);
        boolean moved = result.getState() != null
                && !result.getState().isStructural()
                && result.getState() != before;
        if (!moved) {
            if (result.getRejectReason() != null) {
                throw new BizException(result.getRejectReason());
            }
            if (OrderEventCatalog.allowed(before).contains(command.getEvent())) {
                throw new BizException("事件「" + command.getEvent().getLabel() + "」被守卫拒绝");
            }
            throw new BizException("状态「" + before.getLabel() + "」不能接受事件「" + command.getEvent().getLabel() + "」");
        }
        order.setStatus(result.getState());
        saveLog(orderNo, before, result.getState(), command);
        return assembler.toView(order, false);
    }

    @Transactional(readOnly = true)
    public TradeOrderView get(String orderNo) {
        TradeOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        return assembler.toView(order, false);
    }

    private void saveLog(String orderNo, OrderState from, OrderState to, OrderEventCommand command) {
        TransitionLog log = new TransitionLog();
        log.setMachineType(MACHINE_TYPE);
        log.setMachineId(orderNo);
        log.setFromState(from.name());
        log.setToState(to.name());
        log.setEvent(command.getEvent().name());
        log.setOperator(command.getOperator());
        log.setEventId(command.getEventId());
        log.setRemark(command.getRemark());
        logRepository.save(log);
    }

    private String nextOrderNo() {
        return "T" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}

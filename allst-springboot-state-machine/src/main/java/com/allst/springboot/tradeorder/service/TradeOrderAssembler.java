package com.allst.springboot.tradeorder.service;

import com.allst.springboot.audit.TransitionLog;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderEventCatalog;
import com.allst.springboot.tradeorder.domain.TradeOrder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TradeOrderAssembler {

    public TradeOrderView toView(TradeOrder order, boolean replayed) {
        TradeOrderView view = new TradeOrderView();
        view.setOrderNo(order.getOrderNo());
        view.setBuyerName(order.getBuyerName());
        view.setSkuCode(order.getSkuCode());
        view.setQuantity(order.getQuantity());
        view.setAmount(order.getAmount());
        view.setRiskScore(order.getRiskScore());
        view.setStatus(order.getStatus());
        view.setStatusLabel(order.getStatus().getLabel());
        view.setShipmentNo(order.getShipmentNo());
        view.setPaidAt(order.getPaidAt());
        view.setShippedAt(order.getShippedAt());
        view.setReplayed(replayed);
        view.setAllowedEvents(OrderEventCatalog.allowed(order.getStatus()).stream()
                .map(this::option)
                .collect(Collectors.toList()));
        return view;
    }

    public TransitionLogView toLogView(TransitionLog log) {
        TransitionLogView view = new TransitionLogView();
        view.setFromState(log.getFromState());
        view.setToState(log.getToState());
        view.setEvent(log.getEvent());
        view.setOperator(log.getOperator());
        view.setEventId(log.getEventId());
        view.setRemark(log.getRemark());
        view.setCreatedAt(log.getCreatedAt());
        return view;
    }

    private EventOption option(OrderEvent event) {
        return new EventOption(event.name(), event.getLabel());
    }
}

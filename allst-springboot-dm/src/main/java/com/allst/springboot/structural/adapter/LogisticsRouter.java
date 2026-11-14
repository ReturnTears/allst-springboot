package com.allst.springboot.structural.adapter;

import com.allst.springboot.common.BizException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 路由层：业务只传承运商编码，不需要知道背后有几套 SDK。
 * 依赖的是 LogisticsClient 接口，新增承运商只需再加一个 @Bean。
 */
@Component
public class LogisticsRouter {

    private final Map<String, LogisticsClient> clients;

    public LogisticsRouter(Map<String, LogisticsClient> clients) {
        this.clients = new LinkedHashMap<>(clients);
    }

    public Set<String> carriers() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    public LogisticsClient.ShipmentTicket ship(String carrier, String orderNo, String address) {
        LogisticsClient client = clients.get(carrier);
        if (client == null) {
            throw new BizException("未知承运商: " + carrier);
        }
        return client.create(orderNo, address);
    }
}

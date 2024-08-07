package com.allst.boot.service.impl;

import com.allst.boot.model.OrderBO;
import com.allst.boot.model.OrderDiscount;
import com.allst.boot.service.OrderDiscountService;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Hutu
 * @since 2024-08-07 下午 10:11
 */
@Service
public class OrderDiscountServiceImpl implements OrderDiscountService {
    @Autowired
    private KieContainer kieContainer;
    @Override
    public OrderDiscount getDiscount(OrderBO orderRequest) {
        OrderDiscount orderDiscount = new OrderDiscount();
        // 开启会话
        KieSession kieSession = kieContainer.newKieSession();
        // 设置折扣对象
        kieSession.setGlobal("orderDiscount", orderDiscount);
        // 设置订单对象
        kieSession.insert(orderRequest);
        // 触发规则
        kieSession.fireAllRules();
        //或者  通过规则过滤器实现只执行指定规则
        //kieSession.fireAllRules(new RuleNameEqualsAgendaFilter("Age based discount"));
        // 中止会话
        kieSession.dispose();
        return orderDiscount;
    }
}

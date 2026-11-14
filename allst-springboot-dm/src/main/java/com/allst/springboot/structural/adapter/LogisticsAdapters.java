package com.allst.springboot.structural.adapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把两个第三方 SDK 适配成同一接口，注册为两个 bean。
 * bean 名就是承运商编码，LogisticsRouter 用 Map 注入后按编码路由。
 */
@Configuration
public class LogisticsAdapters {

    @Bean("SF")
    public LogisticsClient sfExpressClient() {
        SfExpressSdk sdk = new SfExpressSdk();
        return new LogisticsClient() {
            @Override
            public String carrier() {
                return "SF";
            }

            @Override
            public ShipmentTicket create(String orderNo, String address) {
                String waybill = sdk.sfCreateOrder(address, orderNo);
                return new ShipmentTicket("SF", waybill, orderNo);
            }
        };
    }

    @Bean("JD")
    public LogisticsClient jdLogisticsClient() {
        JdLogisticsSdk sdk = new JdLogisticsSdk();
        return new LogisticsClient() {
            @Override
            public String carrier() {
                return "JD";
            }

            @Override
            public ShipmentTicket create(String orderNo, String address) {
                JdLogisticsSdk.JdResult result = sdk.createWaybill(orderNo, address);
                return new ShipmentTicket("JD", result.getWaybill(), orderNo);
            }
        };
    }
}

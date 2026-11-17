package com.allst.springboot.structural.adapter;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 适配器（Adapter）——统一多家物流/支付 SDK 的调用形状。
 * <Pre>
 * 场景：顺丰、京东物流各自的 SDK，方法名、参数顺序、返回类型都不同，而且源码不在我们手里。
 * 业务层只认 LogisticsClient，适配器负责把第三方 API 翻译成统一语义。
 * <Pre>
 * 什么时候不用：如果第三方 SDK 本来就是自己设计、能直接改接口，加一层适配器只是多余跳转。
 */
public interface LogisticsClient {

    String carrier();

    ShipmentTicket create(String orderNo, String address);

    /** 统一回执，业务层不再认识任何第三方类型 */
    @Getter
    @AllArgsConstructor
    class ShipmentTicket {
        private final String carrier;
        private final String waybillNo;
        private final String orderNo;
    }
}

/** 模拟顺丰 SDK：地址在前、订单号在后，返回一个字符串单号 */
final class SfExpressSdk {
    String sfCreateOrder(String address, String orderId) {
        return "SF" + Math.abs((orderId + address).hashCode());
    }
}

/** 模拟京东 SDK：参数顺序相反，返回的是自定义对象 */
final class JdLogisticsSdk {
    JdResult createWaybill(String orderId, String address) {
        return new JdResult("JD" + Math.abs((address + orderId).hashCode()));
    }

    static final class JdResult {
        private final String waybill;

        JdResult(String waybill) {
            this.waybill = waybill;
        }

        String getWaybill() {
            return waybill;
        }
    }
}

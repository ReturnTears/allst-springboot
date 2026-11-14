package com.allst.springboot.showcase;

import com.allst.springboot.behavioral.observer.OrderEventListeners;
import com.allst.springboot.common.BizException;
import com.allst.springboot.domain.OrderRecordRepository;
import com.allst.springboot.domain.OrderRequest;
import com.allst.springboot.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 端到端：一次下单把二十多个模式串起来，并验证幂等、校验失败、事务后事件。
 */
@SpringBootTest
@ActiveProfiles("test")
class PlaceOrderFlowTest {

    @Autowired
    private PlaceOrderFacade facade;
    @Autowired
    private OrderRecordRepository repository;
    @Autowired
    private OrderEventListeners eventListeners;

    @Test
    @DisplayName("完整下单：结果正确，trace 覆盖创建/结构/行为三类模式")
    void placeOrderRunsWholeChain() {
        OrderRequest request = request(10, "DOMESTIC", "ALIPAY");
        PlaceOrderResult result = facade.place(request);

        assertThat(result.getOrderNo()).startsWith("DM");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getOriginalAmount()).isEqualByComparingTo("398.00");
        assertThat(result.getDiscount()).isEqualByComparingTo("59.70");
        assertThat(result.getPayable()).isEqualByComparingTo("338.30");
        assertThat(result.getPayChannel()).isEqualTo("ALIPAY");
        assertThat(result.getWaybillNo()).startsWith("SF");
        assertThat(result.isDuplicate()).isFalse();

        assertThat(result.getTrace()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(String.join("|", result.getTrace()))
                .contains("责任链", "组合", "解释器", "策略", "建造者", "抽象工厂",
                        "享元", "装饰器", "原型", "工厂方法", "状态", "命令",
                        "中介者", "观察者", "备忘录", "模板方法", "访问者",
                        "适配器", "桥接", "代理", "单例", "外观");

        assertThat(repository.findByOrderNo(result.getOrderNo())).isPresent();
    }

    @Test
    @DisplayName("同一 requestId 重复提交不产生第二笔订单")
    void duplicateRequestIsIdempotent() {
        OrderRequest request = request(10, "DOMESTIC", "WECHAT");
        long before = repository.count();
        PlaceOrderResult first = facade.place(request);
        PlaceOrderResult second = facade.place(request);
        assertThat(second.isDuplicate()).isTrue();
        assertThat(second.getOrderNo()).isEqualTo(first.getOrderNo());
        assertThat(repository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("风控分过高时责任链拦下请求，不落库")
    void rejectedByChain() {
        long before = repository.count();
        assertThatThrownBy(() -> facade.place(request(95, "DOMESTIC", "ALIPAY")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("下单校验未通过");
        assertThat(repository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("事务提交后才触发仓库通知，事务内同步监听先执行")
    void observersUseCorrectPhase() {
        PlaceOrderResult result = facade.place(request(10, "CROSS_BORDER", "BALANCE"));
        String synchronous = eventListeners.trail().get("inTransaction").toString();
        String afterCommit = eventListeners.trail().get("afterCommit").toString();
        assertThat(synchronous).contains("审计: " + result.getOrderNo());
        assertThat(afterCommit).contains("提交后通知仓库: " + result.getOrderNo());
        assertThat(result.getCompliance()).contains("报关");
    }

    @Test
    @DisplayName("未知业务渠道由抽象工厂拒绝")
    void unknownChannelRejected() {
        assertThatThrownBy(() -> facade.place(request(10, "MARS", "ALIPAY")))
                .isInstanceOf(BizException.class);
    }

    private OrderRequest request(int riskScore, String channel, String payChannel) {
        OrderRequest request = new OrderRequest();
        request.setBuyer("tester");
        request.setSku("SKU-PHONE");
        request.setUnitPrice(new BigDecimal("199.00"));
        request.setQuantity(2);
        request.setVip(true);
        request.setPayChannel(payChannel);
        request.setChannel(channel);
        request.setRequestId("FLOW-" + UUID.randomUUID());
        request.setRiskScore(riskScore);
        return request;
    }
}

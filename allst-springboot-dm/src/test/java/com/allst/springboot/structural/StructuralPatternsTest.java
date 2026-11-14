package com.allst.springboot.structural;

import com.allst.springboot.structural.adapter.LogisticsRouter;
import com.allst.springboot.structural.bridge.NotificationBridge;
import com.allst.springboot.structural.composite.PromotionRuleTree;
import com.allst.springboot.structural.decorator.PriceQuoteService;
import com.allst.springboot.structural.flyweight.SkuSpecRegistry;
import com.allst.springboot.structural.proxy.JdkAuditProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StructuralPatternsTest {

    @Autowired
    private LogisticsRouter logisticsRouter;
    @Autowired
    private NotificationBridge notificationBridge;
    @Autowired
    private PriceQuoteService priceQuoteService;
    @Autowired
    private SkuSpecRegistry skuSpecRegistry;

    @Test
    @DisplayName("适配器：两套 SDK 的差异被挡在统一回执后面")
    void adapter() {
        assertThat(logisticsRouter.carriers()).containsExactlyInAnyOrder("SF", "JD");
        assertThat(logisticsRouter.ship("SF", "A-1", "addr").getCarrier()).isEqualTo("SF");
        assertThat(logisticsRouter.ship("JD", "A-1", "addr").getWaybillNo()).startsWith("JD");
        assertThatThrownBy(() -> logisticsRouter.ship("UNKNOWN", "A-1", "addr"))
                .isInstanceOf(com.allst.springboot.common.BizException.class);
    }

    @Test
    @DisplayName("桥接：消息种类与通道自由组合")
    void bridge() {
        assertThat(notificationBridge.send("ALERT", "EMAIL", "ops@demo", "库存偏低"))
                .contains("EMAIL->").contains("[告警]");
        assertThat(notificationBridge.send("ORDER", "SMS", "13800000000", "A-2"))
                .contains("SMS->").contains("订单 A-2 已支付");
        assertThat(notificationBridge.sent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("组合：规则树按 AND/OR/NOT 求值，跨境单被 NOT 挡住")
    void composite() {
        PromotionRuleTree.EvalContext ctx = new PromotionRuleTree.EvalContext();
        ctx.setAmount(new BigDecimal("150"));
        ctx.setVip(true);
        ctx.setRiskScore(10);
        PromotionRuleTree.RuleNode tree = PromotionRuleTree.couponGate();
        assertThat(tree.describe()).contains("AND", "OR", "NOT");
        assertThat(tree.evaluate(ctx)).isTrue();
        ctx.setChannel("CROSS_BORDER");
        assertThat(tree.evaluate(ctx)).isFalse();
        ctx.setChannel("DOMESTIC");
        ctx.setVip(false);
        ctx.setNewUser(false);
        assertThat(tree.evaluate(ctx)).isFalse();
    }

    @Test
    @DisplayName("装饰器：重复报价只回源一次，命中来自缓存")
    void decorator() {
        // 其它用例可能已经把 SKU-PHONE 预热过，先失效，隔离执行顺序
        priceQuoteService.evict("SKU-PHONE");
        Map<String, Object> before = priceQuoteService.stats();
        int readsBefore = (Integer) before.get("dbReads");
        int hitsBefore = (Integer) before.get("cacheHits");
        assertThat(priceQuoteService.quote("SKU-PHONE")).isEqualByComparingTo("2999.00");
        assertThat(priceQuoteService.quote("SKU-PHONE")).isEqualByComparingTo("2999.00");
        Map<String, Object> after = priceQuoteService.stats();
        assertThat((Integer) after.get("dbReads") - readsBefore).isEqualTo(1);
        assertThat((Integer) after.get("cacheHits") - hitsBefore).isEqualTo(1);
        assertThatThrownBy(() -> priceQuoteService.quote("SKU-NOT-EXIST"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("享元：同一 SKU 共享一份不可变规格")
    void flyweight() {
        SkuSpecRegistry.SkuSpec a = skuSpecRegistry.intern("SKU-UNIQUE-1");
        SkuSpecRegistry.SkuSpec b = skuSpecRegistry.intern("SKU-UNIQUE-1");
        assertThat(a).isSameAs(b);
        assertThat(a.asMap()).containsEntry("sku", "SKU-UNIQUE-1");
        assertThatThrownBy(() -> a.asMap().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(skuSpecRegistry.describe("SKU-PHONE", 2, new BigDecimal("2800"))).contains("本次=2");
    }

    @Test
    @DisplayName("代理：调用前后被记录，异常也被记录")
    void proxy() {
        List<String> audit = new ArrayList<>();
        JdkAuditProxy.StockService stock =
                JdkAuditProxy.wrap(new JdkAuditProxy.DefaultStockService(), audit);
        assertThat(stock.available("SKU-PHONE")).isEqualTo(100);
        stock.deduct("SKU-PHONE", 3);
        assertThat(stock.available("SKU-PHONE")).isEqualTo(97);
        assertThat(audit).contains("BEFORE deduct", "AFTER deduct");
        assertThatThrownBy(() -> stock.deduct("SKU-PHONE", 999))
                .isInstanceOf(IllegalStateException.class);
        assertThat(audit.stream().anyMatch(s -> s.startsWith("FAILED deduct"))).isTrue();
    }
}

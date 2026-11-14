package com.allst.springboot.creational;

import com.allst.springboot.common.BizException;
import com.allst.springboot.creational.abstractfactory.ChannelKitFactory;
import com.allst.springboot.creational.builder.PromotionRule;
import com.allst.springboot.creational.factorymethod.PayChannelFactory;
import com.allst.springboot.creational.prototype.CartDraft;
import com.allst.springboot.creational.prototype.CartDraftProvider;
import com.allst.springboot.creational.prototype.PromotionTemplate;
import com.allst.springboot.creational.prototype.PromotionTemplateRegistry;
import com.allst.springboot.creational.singleton.RuleRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CreationalPatternsTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private RuleRegistry rules;
    @Autowired
    private PayChannelFactory payChannelFactory;
    @Autowired
    private ChannelKitFactory channelKitFactory;
    @Autowired
    private PromotionTemplateRegistry templateRegistry;
    @Autowired
    private CartDraftProvider cartDraftProvider;

    @Test
    @DisplayName("单例：Spring 容器里是同一个实例，规则可整体重载")
    void singleton() {
        assertThat(context.getBean(RuleRegistry.class)).isSameAs(rules);
        assertThat(rules.snapshot()).containsKeys("VIP_DISCOUNT", "FULL_REDUCTION_THRESHOLD",
                "FULL_REDUCTION_AMOUNT", "HIGH_AMOUNT_THRESHOLD");
        long before = rules.reloadCount();
        rules.reload(Collections.singletonMap("TEST_ONLY_RULE", BigDecimal.ONE));
        assertThat(rules.get("TEST_ONLY_RULE")).isEqualByComparingTo("1");
        assertThat(rules.reloadCount()).isEqualTo(before + 1);
        assertThatThrownBy(() -> rules.get("NOT_EXIST")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("工厂方法：三个渠道各自产生命令，未知渠道被拒")
    void factoryMethod() {
        assertThat(payChannelFactory.supportedChannels()).containsExactlyInAnyOrder("ALIPAY", "WECHAT", "BALANCE");
        assertThat(payChannelFactory.pay("ALIPAY", "A-1", new BigDecimal("10")).getChannel()).isEqualTo("ALIPAY");
        assertThat(payChannelFactory.pay("wechat", "A-2", new BigDecimal("10")).getChannel()).isEqualTo("WECHAT");
        assertThat(payChannelFactory.pay("BALANCE", "A-3", new BigDecimal("10")).getSerialNo())
                .isEqualTo("BAL-A-3");
        assertThatThrownBy(() -> payChannelFactory.pay("UNKNOWN", "A-4", BigDecimal.TEN))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("抽象工厂：国内与跨境各给一族组件，规则不串用")
    void abstractFactory() {
        ChannelKitFactory.ChannelKit domestic = channelKitFactory.forChannel("DOMESTIC");
        ChannelKitFactory.ChannelKit crossBorder = channelKitFactory.forChannel("CROSS_BORDER");
        assertThat(domestic.newOrderNo()).startsWith("DM");
        assertThat(crossBorder.newOrderNo()).startsWith("CB");
        assertThat(domestic.settle(new BigDecimal("100"))).isEqualByComparingTo("100.00");
        assertThat(crossBorder.settle(new BigDecimal("100"))).isEqualByComparingTo("109.00");
        assertThat(crossBorder.complianceNote()).contains("报关");
        assertThatThrownBy(() -> channelKitFactory.forChannel("MARS")).isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("建造者：非法组合在 build 阶段被拦，产物不可变")
    void builder() {
        PromotionRule rule = PromotionRule.builder()
                .code("R-1").name("满百减二十")
                .type(PromotionRule.Type.FULL_REDUCTION)
                .threshold(new BigDecimal("100")).value(new BigDecimal("20"))
                .channel("DOMESTIC").priority(1).build();
        assertThat(rule.toString()).contains("R-1");
        assertThat(rule.allowsChannel("DOMESTIC")).isTrue();
        assertThat(rule.allowsChannel("CROSS_BORDER")).isFalse();
        assertThatThrownBy(() -> rule.getChannels().add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> PromotionRule.builder().code("R-2").name("x")
                .type(PromotionRule.Type.FULL_REDUCTION).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("减免金额");
        assertThatThrownBy(() -> PromotionRule.builder().code("R-3").name("x")
                .type(PromotionRule.Type.PERCENT).value(new BigDecimal("1.5")).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("原型：深拷贝不串改；prototype 作用域每次都是新对象")
    void prototype() {
        PromotionTemplate activity = templateRegistry.newActivity("DOUBLE11", "双十二活动");
        activity.addSku("SKU-NEW");
        activity.putExtra("owner", "ops");
        assertThat(activity.getSkuWhitelist()).contains("SKU-NEW");
        assertThat(templateRegistry.inspect("DOUBLE11").getSkuWhitelist()).doesNotContain("SKU-NEW");
        assertThat(templateRegistry.inspect("DOUBLE11").getExtras()).doesNotContainKey("owner");

        CartDraft first = cartDraftProvider.newDraft();
        CartDraft second = cartDraftProvider.newDraft();
        first.add("SKU-PHONE");
        assertThat(first).isNotSameAs(second);
        assertThat(second.items()).isEmpty();
    }
}

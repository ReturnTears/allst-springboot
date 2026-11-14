package com.allst.springboot;

import com.allst.springboot.showcase.PatternCatalog;
import com.allst.springboot.showcase.PlaceOrderFacade;
import com.allst.springboot.support.InMemoryKeyValueStore;
import com.allst.springboot.support.KeyValueStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文能起来，是这一模块最基本的验收：默认不依赖 MySQL 和 Redis。
 */
@SpringBootTest
@ActiveProfiles("test")
class DmContextLoadTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PlaceOrderFacade facade;

    @Test
    @DisplayName("test profile 使用 H2 + 内存 KV，无需外部服务")
    void contextLoadsWithoutExternalServices() {
        assertThat(context.getBean(KeyValueStore.class)).isInstanceOf(InMemoryKeyValueStore.class);
        assertThat(facade).isNotNull();
        assertThat(facade.overview()).containsKeys("validationChecks", "discountStrategies",
                "payChannels", "logisticsCarriers", "stateTransitions", "rules");
    }

    @Test
    @DisplayName("速查表覆盖 23 种 GoF 模式 + 3 条 Spring 机制")
    void catalogCoversAllPatterns() {
        assertThat(PatternCatalog.all()).hasSize(26);
        assertThat(PatternCatalog.all().stream()
                .filter(p -> "创建型".equals(p.getCategory())).count()).isEqualTo(5);
        assertThat(PatternCatalog.all().stream()
                .filter(p -> "结构型".equals(p.getCategory())).count()).isEqualTo(7);
        assertThat(PatternCatalog.all().stream()
                .filter(p -> "行为型".equals(p.getCategory())).count()).isEqualTo(11);
    }
}

package com.allst.springboot.creational.prototype;

import com.allst.springboot.common.BizException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 原型管理器：模板只读，每次发活动返回一份副本。
 */
@Component
public class PromotionTemplateRegistry {

    private final Map<String, PromotionTemplate> prototypes = new LinkedHashMap<>();

    public PromotionTemplateRegistry() {
        prototypes.put("DOUBLE11", new PromotionTemplate("DOUBLE11", "双十一基础模板",
                new BigDecimal("200"), new BigDecimal("30"),
                Arrays.asList("SKU-PHONE", "SKU-TV"),
                Collections.singletonMap("banner", "双11"), true));
        prototypes.put("NEW_USER", new PromotionTemplate("NEW_USER", "新人专享模板",
                new BigDecimal("50"), new BigDecimal("15"),
                Collections.singletonList("SKU-GIFT"),
                Collections.singletonMap("channel", "APP"), false));
    }

    public PromotionTemplate newActivity(String templateCode, String activityName) {
        PromotionTemplate template = prototypes.get(templateCode);
        if (template == null) {
            throw new BizException("不存在活动模板: " + templateCode);
        }
        return template.copyAs(activityName);
    }

    /** 只读视图，避免调用方拿到原型本体 */
    public PromotionTemplate inspect(String templateCode) {
        PromotionTemplate template = prototypes.get(templateCode);
        if (template == null) {
            throw new BizException("不存在活动模板: " + templateCode);
        }
        return template;
    }
}

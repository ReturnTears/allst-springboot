package com.allst.springboot.creational.prototype;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 原型（Prototype）——大促活动模板的复制。
 *
 * 场景：运营每次做活动，都是「从上一季模板复制一份，改活动名、改 SKU 白名单、改时间」。
 * 重新按十几个字段从数据库装配一遍既慢又容易漏；直接引用同一个对象则会串改。
 *
 * 这里刻意用深拷贝：浅拷贝只复制 List/Map 的引用，一次活动往白名单加 SKU，
 * 会把别的活动一起改掉。这是原型模式在业务代码里最常见的坑。
 */
@Getter
public final class PromotionTemplate {

    private final String code;
    private final String name;
    private final BigDecimal threshold;
    private final BigDecimal reduction;
    private final List<String> skuWhitelist;
    private final Map<String, String> extras;
    private final boolean stackable;

    public PromotionTemplate(String code, String name, BigDecimal threshold, BigDecimal reduction,
                             List<String> skuWhitelist, Map<String, String> extras, boolean stackable) {
        this.code = code;
        this.name = name;
        this.threshold = threshold;
        this.reduction = reduction;
        this.skuWhitelist = new ArrayList<>(skuWhitelist);
        this.extras = new LinkedHashMap<>(extras);
        this.stackable = stackable;
    }

    private PromotionTemplate(PromotionTemplate source, String newName) {
        this.code = source.code;
        this.name = newName;
        this.threshold = source.threshold;
        this.reduction = source.reduction;
        this.skuWhitelist = new ArrayList<>(source.skuWhitelist);
        this.extras = new LinkedHashMap<>(source.extras);
        this.stackable = source.stackable;
    }

    /** 生产用副本：可变集合重新分配，互不干扰 */
    public PromotionTemplate copyAs(String activityName) {
        return new PromotionTemplate(this, activityName);
    }

    public void addSku(String sku) {
        skuWhitelist.add(sku);
    }

    public void putExtra(String key, String value) {
        extras.put(key, value);
    }
}

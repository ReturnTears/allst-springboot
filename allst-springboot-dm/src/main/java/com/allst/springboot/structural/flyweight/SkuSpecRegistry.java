package com.allst.springboot.structural.flyweight;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 享元（Flyweight）——共享 SKU 规格这类「大量重复、且不可变」的内部状态。
 *
 * 场景：一个订单有几百行，每行都要显示品牌、类目、计税单位、材质、售后政策，
 * 这些元数据与「这一单买了几件、卖了多少钱」无关。每行 new 一份会重复占内存，
 * 每行查一次库又会放大 IO。享元池按 SKU 共享一份不可变 SkuSpec，
 * 变化的部分（数量、成交价、买家）作为外部状态由调用方传入。
 *
 * 和单例的区别：单例全局只有一个实例，享元是「每个 key 一个」的一族共享对象。
 * 和缓存的区别：缓存是为了省一次查询、可以随时失效；享元的重点是对象结构里
 * 区分内部/外部状态，共享的是值本身。
 */
@Component
public class SkuSpecRegistry {

    private final Map<String, SkuSpec> pool = new ConcurrentHashMap<>();

    public SkuSpec intern(String sku) {
        return pool.computeIfAbsent(sku, SkuSpecRegistry::load);
    }

    public int pooledSize() {
        return pool.size();
    }

    /** 外部状态由调用方传入，不写进享元对象 */
    public String describe(String sku, int quantity, BigDecimal dealPrice) {
        SkuSpec spec = intern(sku);
        return spec.getSku() + "/" + spec.getBrand() + "/" + spec.getCategory()
                + " 单位=" + spec.getUnit() + " 税率=" + spec.getTaxRate()
                + " 本次=" + quantity + " 件 @ " + dealPrice.toPlainString();
    }

    private static SkuSpec load(String sku) {
        // 真实项目里这里是一次字典表/商品中心查询
        String brand = sku.startsWith("SKU-PHONE") ? "AllstPhone" : "Allst";
        String category = sku.contains("BOOK") ? "图书" : "实物商品";
        return new SkuSpec(sku, brand, category, "件", new BigDecimal("0.13"), "7天无理由");
    }

    public static final class SkuSpec {
        private final String sku;
        private final String brand;
        private final String category;
        private final String unit;
        private final BigDecimal taxRate;
        private final String afterSale;

        private SkuSpec(String sku, String brand, String category, String unit,
                        BigDecimal taxRate, String afterSale) {
            this.sku = sku;
            this.brand = brand;
            this.category = category;
            this.unit = unit;
            this.taxRate = taxRate;
            this.afterSale = afterSale;
        }

        public String getSku() {
            return sku;
        }

        public String getBrand() {
            return brand;
        }

        public String getCategory() {
            return category;
        }

        public String getUnit() {
            return unit;
        }

        public BigDecimal getTaxRate() {
            return taxRate;
        }

        public String getAfterSale() {
            return afterSale;
        }

        public Map<String, String> asMap() {
            Map<String, String> map = new java.util.LinkedHashMap<>();
            map.put("sku", sku);
            map.put("brand", brand);
            map.put("category", category);
            map.put("unit", unit);
            map.put("taxRate", taxRate.toPlainString());
            map.put("afterSale", afterSale);
            return Collections.unmodifiableMap(map);
        }
    }
}

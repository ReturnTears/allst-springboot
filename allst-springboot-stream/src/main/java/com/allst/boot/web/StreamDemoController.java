package com.allst.boot.web;

import com.allst.boot.collector.CollectorShowcase;
import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderView;
import com.allst.boot.functional.FunctionShowcase;
import com.allst.boot.functional.PredicateShowcase;
import com.allst.boot.mechanics.MechanicsShowcase;
import com.allst.boot.strategy.DiscountService;
import com.allst.boot.support.Orders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 演示入口：把各层案例暴露成 HTTP 接口，便于直接观察结果。
 * <p>
 * 控制器保持极薄——不写任何 Stream 逻辑，只转发。
 * 这样每个案例都能脱离 Web 层单测，这也是「可测试性」的体现。
 */
@RestController
@RequestMapping("/api/stream")
public class StreamDemoController {

    private final PredicateShowcase predicates;
    private final FunctionShowcase functions;
    private final CollectorShowcase collectors;
    private final MechanicsShowcase mechanics;
    private final DiscountService discounts;

    public StreamDemoController(PredicateShowcase predicates, FunctionShowcase functions,
                                CollectorShowcase collectors, MechanicsShowcase mechanics,
                                DiscountService discounts) {
        this.predicates = predicates;
        this.functions = functions;
        this.collectors = collectors;
        this.mechanics = mechanics;
        this.discounts = discounts;
    }

    /** 可组合筛选：按城市与最低金额过滤。 */
    @GetMapping("/filter")
    public List<Order> filter(@RequestParam(required = false) String city,
                              @RequestParam(defaultValue = "0") BigDecimal minAmount) {
        List<java.util.function.Predicate<Order>> conditions = new java.util.ArrayList<>();
        if (city != null && !city.isBlank()) {
            conditions.add(predicates.inCity(city));
        }
        conditions.add(predicates.amountAtLeast(minAmount));
        return predicates.filter(Orders.sample(), conditions);
    }

    /** 跨集合关联：订单补会员等级后转视图。 */
    @GetMapping("/views")
    public List<OrderView> views() {
        return functions.toViews(Orders.sample(), Orders.members());
    }

    /** 城市成交额报表。 */
    @GetMapping("/revenue-by-city")
    public Map<String, BigDecimal> revenueByCity() {
        return collectors.revenueByCity(Orders.sample());
    }

    /** 城市 → 商品名集合，演示 flatMapping。 */
    @GetMapping("/products-by-city")
    public Map<String, java.util.Set<String>> productsByCity() {
        return collectors.productNamesByCity(Orders.sample());
    }

    /** 券种优惠统计。 */
    @GetMapping("/coupon-savings")
    public Map<String, BigDecimal> couponSavings() {
        return discounts.savingsByCoupon(Orders.sample());
    }

    /** 明细摊平并带订单号。 */
    @GetMapping("/items")
    public List<String> items() {
        return mechanics.itemsWithOrderNo(Orders.sample());
    }
}
package com.allst.springboot.showcase;

import com.allst.springboot.behavioral.command.CommandInvoker;
import com.allst.springboot.behavioral.iterator.PagedOrderCursor;
import com.allst.springboot.behavioral.memento.SnapshotCareTaker;
import com.allst.springboot.behavioral.observer.OrderEventListeners;
import com.allst.springboot.common.BizException;
import com.allst.springboot.common.Result;
import com.allst.springboot.creational.singleton.RuleRegistry;
import com.allst.springboot.domain.OrderRecord;
import com.allst.springboot.domain.OrderRecordRepository;
import com.allst.springboot.domain.OrderRequest;
import com.allst.springboot.structural.bridge.NotificationBridge;
import com.allst.springboot.structural.decorator.PriceQuoteService;
import com.allst.springboot.structural.flyweight.SkuSpecRegistry;
import com.allst.springboot.structural.proxy.ServiceTimingAspect;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示入口。每个接口都对应 docs/09-api-and-run.md 里的一条 curl。
 */
@RestController
@RequestMapping("/api/dm")
public class DmShowcaseController {

    private final PlaceOrderFacade facade;
    private final DemoRunner demoRunner;
    private final OrderRecordRepository repository;
    private final OrderEventListeners eventListeners;
    private final PriceQuoteService priceQuoteService;
    private final CommandInvoker commandInvoker;
    private final SnapshotCareTaker snapshotCareTaker;
    private final ServiceTimingAspect timingAspect;
    private final RuleRegistry ruleRegistry;
    private final SkuSpecRegistry skuSpecRegistry;
    private final NotificationBridge notificationBridge;

    public DmShowcaseController(PlaceOrderFacade facade,
                                DemoRunner demoRunner,
                                OrderRecordRepository repository,
                                OrderEventListeners eventListeners,
                                PriceQuoteService priceQuoteService,
                                CommandInvoker commandInvoker,
                                SnapshotCareTaker snapshotCareTaker,
                                ServiceTimingAspect timingAspect,
                                RuleRegistry ruleRegistry,
                                SkuSpecRegistry skuSpecRegistry,
                                NotificationBridge notificationBridge) {
        this.facade = facade;
        this.demoRunner = demoRunner;
        this.repository = repository;
        this.eventListeners = eventListeners;
        this.priceQuoteService = priceQuoteService;
        this.commandInvoker = commandInvoker;
        this.snapshotCareTaker = snapshotCareTaker;
        this.timingAspect = timingAspect;
        this.ruleRegistry = ruleRegistry;
        this.skuSpecRegistry = skuSpecRegistry;
        this.notificationBridge = notificationBridge;
    }

    /** 走完整条下单链路，返回里带 trace，能逐条对照文档 */
    @PostMapping("/orders")
    public Result<PlaceOrderResult> place(@Valid @RequestBody OrderRequest request) {
        return Result.ok(facade.place(request));
    }

    @GetMapping("/orders/{orderNo}")
    public Result<OrderRecord> get(@PathVariable String orderNo) {
        OrderRecord record = repository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BizException(404, "订单不存在: " + orderNo));
        return Result.ok(record);
    }

    /** 迭代器演示：按批扫描订单，不一次载入全部 */
    @GetMapping("/orders")
    public Result<Map<String, Object>> scan(@RequestParam(defaultValue = "2") int batchSize) {
        PagedOrderCursor cursor = new PagedOrderCursor(repository, batchSize);
        List<String> orderNos = new ArrayList<>();
        while (cursor.hasNext()) {
            orderNos.add(cursor.next().getOrderNo());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("batchSize", batchSize);
        out.put("total", orderNos.size());
        out.put("batchesRead", cursor.batchesRead());
        out.put("orderNos", orderNos);
        return Result.ok(out);
    }

    /** 场景 → 模式 → 解决的问题 → 对应类，和 docs/02-scenario-map.md 同源 */
    @GetMapping("/patterns")
    public Result<List<PatternCatalog.PatternInfo>> patterns() {
        return Result.ok(PatternCatalog.all());
    }

    @GetMapping("/demo")
    public Result<List<String>> demoKeys() {
        return Result.ok(demoRunner.keys());
    }

    @GetMapping("/demo/{pattern}")
    public Result<Object> demo(@PathVariable String pattern) {
        return Result.ok(demoRunner.run(pattern));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(facade.overview());
    }

    /** 把有状态的几个模式的运行痕迹集中暴露出来 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("rules", ruleRegistry.snapshot());
        stats.put("skuFlyweightPoolSize", skuSpecRegistry.pooledSize());
        stats.put("decorator", priceQuoteService.stats());
        stats.put("springAopProxy", timingAspect.stats());
        stats.put("events", eventListeners.trail());
        stats.put("commands", commandInvoker.history());
        stats.put("snapshots", snapshotCareTaker.labels());
        stats.put("bridgeSent", notificationBridge.sent());
        stats.put("orderCount", repository.count());
        return Result.ok(stats);
    }
}

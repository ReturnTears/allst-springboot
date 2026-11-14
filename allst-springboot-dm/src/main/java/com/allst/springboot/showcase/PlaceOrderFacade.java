package com.allst.springboot.showcase;

import com.allst.springboot.behavioral.chain.OrderValidationChain;
import com.allst.springboot.behavioral.chain.OrderValidationChain.ValidationResult;
import com.allst.springboot.behavioral.command.CommandInvoker;
import com.allst.springboot.behavioral.command.CommandInvoker.CommandResult;
import com.allst.springboot.behavioral.command.OrderCommand;
import com.allst.springboot.behavioral.interpreter.PromotionExpression;
import com.allst.springboot.behavioral.mediator.OrderMediator;
import com.allst.springboot.behavioral.memento.CartSnapshot.CartEditor;
import com.allst.springboot.behavioral.memento.CartSnapshot.Memento;
import com.allst.springboot.behavioral.memento.SnapshotCareTaker;
import com.allst.springboot.behavioral.observer.OrderPlacedEvent;
import com.allst.springboot.behavioral.state.OrderStateMachine;
import com.allst.springboot.behavioral.state.OrderStateMachine.Event;
import com.allst.springboot.behavioral.strategy.DiscountStrategyRouter;
import com.allst.springboot.behavioral.strategy.DiscountStrategyRouter.DiscountContext;
import com.allst.springboot.behavioral.template.ReceiptService;
import com.allst.springboot.behavioral.template.ReceiptTemplate.Context;
import com.allst.springboot.behavioral.template.ReceiptTemplate.Receipt;
import com.allst.springboot.behavioral.visitor.OrderLineVisitorDemo;
import com.allst.springboot.common.BizException;
import com.allst.springboot.creational.abstractfactory.ChannelKitFactory;
import com.allst.springboot.creational.abstractfactory.ChannelKitFactory.ChannelKit;
import com.allst.springboot.creational.builder.PromotionRule;
import com.allst.springboot.creational.factorymethod.PayChannelFactory;
import com.allst.springboot.creational.factorymethod.PayChannelFactory.PayReceipt;
import com.allst.springboot.creational.prototype.CartDraftProvider;
import com.allst.springboot.creational.prototype.PromotionTemplate;
import com.allst.springboot.creational.prototype.PromotionTemplateRegistry;
import com.allst.springboot.creational.singleton.RuleRegistry;
import com.allst.springboot.domain.OrderRecord;
import com.allst.springboot.domain.OrderRecordRepository;
import com.allst.springboot.domain.OrderRequest;
import com.allst.springboot.domain.OrderStatus;
import com.allst.springboot.structural.adapter.LogisticsClient;
import com.allst.springboot.structural.adapter.LogisticsRouter;
import com.allst.springboot.structural.bridge.NotificationBridge;
import com.allst.springboot.structural.composite.PromotionRuleTree;
import com.allst.springboot.structural.decorator.PriceQuoteService;
import com.allst.springboot.structural.flyweight.SkuSpecRegistry;
import com.allst.springboot.structural.proxy.JdkAuditProxy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 外观（Facade）——一次下单把 20 多个模式串起来，Controller 只调这一个方法。
 *
 * 这个类的价值不是「代码多」，而是展示真实业务里模式很少单独出现：
 * 一条下单链路会同时需要校验链、策略、规则树、表达式、创建类模式、状态、
 * 命令、事件、持久化、审计、代理。外观把这些编排细节挡在 HTTP 层之外。
 *
 * 事务边界也在这里：命令落库和状态回写在同一个事务，事务提交后才发仓库通知
 * （见 OrderEventListeners 的 AFTER_COMMIT）。AOP 计时切面挂在showcase.PlaceOrderFacade 上。
 */
@Component
public class PlaceOrderFacade {

    private final OrderValidationChain validationChain;
    private final DiscountStrategyRouter discountRouter;
    private final OrderStateMachine stateMachine;
    private final ChannelKitFactory channelKitFactory;
    private final PayChannelFactory payChannelFactory;
    private final RuleRegistry ruleRegistry;
    private final SkuSpecRegistry skuSpecRegistry;
    private final PriceQuoteService priceQuoteService;
    private final CommandInvoker commandInvoker;
    private final OrderRecordRepository repository;
    private final OrderMediator mediator;
    private final SnapshotCareTaker snapshotCareTaker;
    private final ReceiptService receiptService;
    private final LogisticsRouter logisticsRouter;
    private final NotificationBridge notificationBridge;
    private final PromotionTemplateRegistry templateRegistry;
    private final CartDraftProvider cartDraftProvider;
    private final ApplicationEventPublisher publisher;

    public PlaceOrderFacade(OrderValidationChain validationChain,
                            DiscountStrategyRouter discountRouter,
                            OrderStateMachine stateMachine,
                            ChannelKitFactory channelKitFactory,
                            PayChannelFactory payChannelFactory,
                            RuleRegistry ruleRegistry,
                            SkuSpecRegistry skuSpecRegistry,
                            PriceQuoteService priceQuoteService,
                            CommandInvoker commandInvoker,
                            OrderRecordRepository repository,
                            OrderMediator mediator,
                            SnapshotCareTaker snapshotCareTaker,
                            ReceiptService receiptService,
                            LogisticsRouter logisticsRouter,
                            NotificationBridge notificationBridge,
                            PromotionTemplateRegistry templateRegistry,
                            CartDraftProvider cartDraftProvider,
                            ApplicationEventPublisher publisher) {
        this.validationChain = validationChain;
        this.discountRouter = discountRouter;
        this.stateMachine = stateMachine;
        this.channelKitFactory = channelKitFactory;
        this.payChannelFactory = payChannelFactory;
        this.ruleRegistry = ruleRegistry;
        this.skuSpecRegistry = skuSpecRegistry;
        this.priceQuoteService = priceQuoteService;
        this.commandInvoker = commandInvoker;
        this.repository = repository;
        this.mediator = mediator;
        this.snapshotCareTaker = snapshotCareTaker;
        this.receiptService = receiptService;
        this.logisticsRouter = logisticsRouter;
        this.notificationBridge = notificationBridge;
        this.templateRegistry = templateRegistry;
        this.cartDraftProvider = cartDraftProvider;
        this.publisher = publisher;
    }

    @Transactional
    public PlaceOrderResult place(OrderRequest request) {
        List<String> trace = new ArrayList<>();

        // 0) 外观层的幂等：同一 requestId 不产生第二笔订单
        Optional<OrderRecord> existing = repository.findByRequestId(request.getRequestId());
        if (existing.isPresent()) {
            trace.add("外观: 命中 requestId 幂等，直接返回原订单");
            return fromRecord(existing.get(), true, trace);
        }

        // 1) 责任链
        ValidationResult validation = validationChain.validate(request);
        trace.add("责任链: " + (validation.isPassed()
                ? "通过 " + validation.getPassedChecks().size() + " 关"
                : "被 [" + validation.getFailedAt() + "] 拦下 - " + validation.getReason()));
        if (!validation.isPassed()) {
            throw new BizException("下单校验未通过: " + validation.getReason());
        }

        // 2) 组合规则树 + 3) 解释器
        PromotionRuleTree.EvalContext treeCtx = new PromotionRuleTree.EvalContext();
        treeCtx.setAmount(request.originalAmount());
        treeCtx.setVip(request.isVip());
        treeCtx.setNewUser("NEW10".equals(request.getCouponCode()));
        treeCtx.setChannel(request.getChannel());
        treeCtx.setRiskScore(request.getRiskScore());
        PromotionRuleTree.RuleNode tree = PromotionRuleTree.couponGate();
        trace.add("组合: " + tree.describe() + " => " + tree.evaluate(treeCtx));

        PromotionExpression.Context exprCtx = new PromotionExpression.Context();
        exprCtx.setAmount(request.originalAmount());
        exprCtx.setVip(request.isVip());
        exprCtx.setNewUser("NEW10".equals(request.getCouponCode()));
        exprCtx.setRiskScore(request.getRiskScore());
        exprCtx.setChannel(request.getChannel());
        boolean expressionHit = PromotionExpression.evaluate(PromotionExpression.Demo.SPEL, exprCtx);
        trace.add("解释器(SpEL): " + PromotionExpression.Demo.SPEL + " => " + expressionHit);

        // 4) 策略（阈值来自单例规则缓存）
        DiscountContext discountCtx = new DiscountContext();
        discountCtx.setOriginalAmount(request.originalAmount());
        discountCtx.setVip(request.isVip());
        discountCtx.setNewUser("NEW10".equals(request.getCouponCode()));
        discountCtx.setCouponCode(request.getCouponCode());
        discountCtx.setChannel(request.getChannel());
        String best = discountRouter.bestType(discountCtx);
        BigDecimal discount = discountRouter.apply(best, discountCtx);
        BigDecimal payable = request.originalAmount().subtract(discount);
        trace.add("策略: 命中 " + best + "，优惠 " + discount.toPlainString());

        // 5) 建造者：把这次生效的规则固化成不可变对象
        PromotionRule rule = PromotionRule.builder()
                .code("AUTO-" + request.getRequestId())
                .name("下单自动命中规则")
                .type(PromotionRule.Type.FULL_REDUCTION)
                .threshold(ruleRegistry.get("FULL_REDUCTION_THRESHOLD"))
                .value(discount)
                .channel(request.getChannel())
                .priority(10)
                .expression(PromotionExpression.Demo.SPEL)
                .build();
        trace.add("建造者: " + rule);

        // 6) 抽象工厂：同渠道的一整套组件
        ChannelKit kit = channelKitFactory.forChannel(request.getChannel());
        String orderNo = kit.newOrderNo();
        BigDecimal settled = kit.settle(payable);
        trace.add("抽象工厂: " + orderNo + " / " + kit.complianceNote());

        // 7) 享元 + 8) 装饰器（缓存报价）
        String spec = skuSpecRegistry.describe(request.getSku(), request.getQuantity(), request.getUnitPrice());
        trace.add("享元: " + spec + "，池大小 " + skuSpecRegistry.pooledSize());
        String catalogNote;
        try {
            catalogNote = "目录价 " + priceQuoteService.quote(request.getSku()).toPlainString();
        } catch (IllegalArgumentException e) {
            catalogNote = "目录中无此 SKU，跳过报价";
        }
        trace.add("装饰器: " + catalogNote + " / " + priceQuoteService.stats());

        // 9) 原型：活动模板复制 + prototype 作用域的购物车草稿
        PromotionTemplate activity = templateRegistry.newActivity("DOUBLE11", "活动-" + orderNo);
        activity.addSku(request.getSku());
        trace.add("原型: " + activity.getName() + " 白名单 " + activity.getSkuWhitelist().size() + " 个 SKU");
        trace.add("原型(作用域): 购物车草稿 " + cartDraftProvider.newDraft().items());

        // 10) 工厂方法：支付命令
        PayReceipt payReceipt = payChannelFactory.pay(request.getPayChannel(), orderNo, settled);
        trace.add("工厂方法: " + payReceipt.getChannel() + " 流水 " + payReceipt.getSerialNo());

        // 11) 状态
        OrderStatus status = stateMachine.fire(OrderStatus.CREATED, Event.PAY);
        trace.add("状态: CREATED --PAY--> " + status);

        // 12) 命令：把落库这件动作封装成可幂等、可撤销的命令
        OrderRecord record = newRecord(request, orderNo, settled, discount, status, payReceipt);
        Map<String, Object> commandContext = new HashMap<>();
        commandContext.put("record", record);
        OrderCommand saveCommand = new OrderCommand() {
            @Override
            public String id() {
                return "SAVE-" + orderNo;
            }

            @Override
            public int maxAttempts() {
                return 1;
            }

            @Override
            public String execute(Map<String, Object> context) {
                repository.save((OrderRecord) context.get("record"));
                return "落库 " + orderNo;
            }

            @Override
            public void undo(Map<String, Object> context) {
                repository.delete((OrderRecord) context.get("record"));
            }
        };
        CommandResult commandResult = commandInvoker.run(saveCommand, commandContext);
        trace.add("命令: " + commandResult.getStatus() + "，尝试 " + commandResult.getAttempts() + " 次");

        // 13) 中介者：协调库存/积分/通知/审计
        List<String> colleagueSteps = mediator.paid(orderNo, settled);
        trace.add("中介者: " + colleagueSteps.size() + " 个同事按序执行");

        // 14) 观察者：事务内同步监听 + 提交后监听
        publisher.publishEvent(new OrderPlacedEvent(orderNo, request.getBuyer(), settled, request.getChannel()));
        trace.add("观察者: 发布 OrderPlacedEvent（仓库通知在 AFTER_COMMIT）");

        // 15) 备忘录
        CartEditor editor = new CartEditor();
        editor.add(request.getSku(), request.getQuantity(), request.getUnitPrice());
        Memento snapshot = editor.save("下单前购物车 " + orderNo);
        snapshotCareTaker.push(snapshot);
        trace.add("备忘录: 保存快照 " + snapshot.getLabel());

        // 16) 模板方法：按渠道生成回执
        Context receiptCtx = new Context();
        receiptCtx.setOrderNo(orderNo);
        receiptCtx.setAmount(settled);
        receiptCtx.setBuyer(request.getBuyer());
        Receipt receiptDoc = receiptService.generate(request.getChannel(), receiptCtx);
        trace.add("模板方法: " + receiptDoc.getTrail());

        // 17) 访问者：同一批订单行换一套口径重算
        Map<String, Object> visitorSummary = OrderLineVisitorDemo.summarize(OrderLineVisitorDemo.sampleItems());
        trace.add("访问者: " + visitorSummary);

        // 18) 适配器：屏蔽第三方物流 SDK 差异
        LogisticsClient.ShipmentTicket ticket = logisticsRouter.ship("SF", orderNo, "默认收货地址");
        trace.add("适配器: " + ticket.getCarrier() + " 运单 " + ticket.getWaybillNo());

        // 19) 桥接：消息种类 × 发送通道
        String notification = notificationBridge.send("ORDER", "SMS", request.getBuyer(), orderNo);
        trace.add("桥接: " + notification);

        // 20) 代理：JDK 动态代理记录调用前后
        List<String> auditLog = new ArrayList<>();
        JdkAuditProxy.StockService stock =
                JdkAuditProxy.wrap(new JdkAuditProxy.DefaultStockService(), auditLog);
        stock.available(request.getSku());
        trace.add("代理: " + auditLog);

        // 21) 单例规则缓存 + 外观自身（AOP 计时切面在这一层外面）
        trace.add("单例: 当前规则 " + ruleRegistry.snapshot().keySet());
        trace.add("外观: 结束，事务提交后触发提交后事件");

        return PlaceOrderResult.builder()
                .orderNo(orderNo)
                .status(status)
                .originalAmount(request.originalAmount())
                .discount(discount)
                .payable(settled)
                .strategy(best)
                .payChannel(payReceipt.getChannel())
                .paySerialNo(payReceipt.getSerialNo())
                .compliance(kit.complianceNote())
                .waybillNo(ticket.getWaybillNo())
                .trace(trace)
                .duplicate(false)
                .build();
    }

    /** 给迭代器/查询接口用的一条完整链路视图 */
    public Map<String, Object> overview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("validationChecks", validationChain.checkNames());
        overview.put("discountStrategies", discountRouter.types());
        overview.put("payChannels", payChannelFactory.supportedChannels());
        overview.put("logisticsCarriers", logisticsRouter.carriers());
        overview.put("receiptChannels", receiptService.channels());
        overview.put("stateTransitions", stateMachine.transitionTable());
        overview.put("mediatorColleagues", mediator.colleagueNames());
        overview.put("rules", ruleRegistry.snapshot());
        overview.put("skuFlyweightPoolSize", skuSpecRegistry.pooledSize());
        return overview;
    }

    private OrderRecord newRecord(OrderRequest request, String orderNo, BigDecimal settled,
                                  BigDecimal discount, OrderStatus status, PayReceipt payReceipt) {
        OrderRecord record = new OrderRecord();
        record.setOrderNo(orderNo);
        record.setBuyer(request.getBuyer());
        record.setSku(request.getSku());
        record.setQuantity(request.getQuantity());
        record.setOriginalAmount(request.originalAmount());
        record.setDiscount(discount);
        record.setPayable(settled);
        record.setStatus(status);
        record.setPayChannel(payReceipt.getChannel());
        record.setRequestId(request.getRequestId());
        record.setCreatedAt(Instant.now());
        return record;
    }

    private PlaceOrderResult fromRecord(OrderRecord record, boolean duplicate, List<String> trace) {
        return PlaceOrderResult.builder()
                .orderNo(record.getOrderNo())
                .status(record.getStatus())
                .originalAmount(record.getOriginalAmount())
                .discount(record.getDiscount())
                .payable(record.getPayable())
                .strategy("命中幂等，复用上次结果")
                .payChannel(record.getPayChannel())
                .paySerialNo("-")
                .compliance("-")
                .waybillNo("-")
                .trace(Arrays.asList(trace.isEmpty() ? "" : trace.get(0)))
                .duplicate(duplicate)
                .build();
    }
}

package com.allst.springboot.showcase;

import com.allst.springboot.behavioral.chain.OrderValidationChain;
import com.allst.springboot.behavioral.command.CommandInvoker;
import com.allst.springboot.behavioral.command.OrderCommand;
import com.allst.springboot.behavioral.interpreter.PromotionExpression;
import com.allst.springboot.behavioral.iterator.PagedOrderCursor;
import com.allst.springboot.behavioral.mediator.OrderMediator;
import com.allst.springboot.behavioral.memento.CartSnapshot.CartEditor;
import com.allst.springboot.behavioral.memento.SnapshotCareTaker;
import com.allst.springboot.behavioral.observer.OrderEventListeners;
import com.allst.springboot.behavioral.state.OrderStateMachine;
import com.allst.springboot.behavioral.state.OrderStateMachine.Event;
import com.allst.springboot.behavioral.strategy.DiscountStrategyRouter;
import com.allst.springboot.behavioral.strategy.DiscountStrategyRouter.DiscountContext;
import com.allst.springboot.behavioral.template.ReceiptService;
import com.allst.springboot.behavioral.template.ReceiptTemplate.Context;
import com.allst.springboot.behavioral.visitor.OrderLineVisitorDemo;
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
import com.allst.springboot.structural.proxy.ServiceTimingAspect;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * /api/dm/demo/{pattern} 的分发器：每个模式都能单独跑一次，不必先走完整下单链路。
 * 返回的是可观察的结果，方便和 docs 里的说明对照。
 */
@Component
public class DemoRunner {

    private final RuleRegistry rules;
    private final PayChannelFactory payChannelFactory;
    private final ChannelKitFactory channelKitFactory;
    private final PromotionTemplateRegistry templateRegistry;
    private final CartDraftProvider cartDraftProvider;
    private final LogisticsRouter logisticsRouter;
    private final NotificationBridge notificationBridge;
    private final PriceQuoteService priceQuoteService;
    private final SkuSpecRegistry skuSpecRegistry;
    private final OrderValidationChain validationChain;
    private final CommandInvoker commandInvoker;
    private final DiscountStrategyRouter discountRouter;
    private final OrderStateMachine stateMachine;
    private final ReceiptService receiptService;
    private final OrderMediator mediator;
    private final SnapshotCareTaker snapshotCareTaker;
    private final OrderEventListeners eventListeners;
    private final OrderRecordRepository repository;
    private final PlaceOrderFacade facade;
    private final ServiceTimingAspect timingAspect;

    public DemoRunner(RuleRegistry rules,
                      PayChannelFactory payChannelFactory,
                      ChannelKitFactory channelKitFactory,
                      PromotionTemplateRegistry templateRegistry,
                      CartDraftProvider cartDraftProvider,
                      LogisticsRouter logisticsRouter,
                      NotificationBridge notificationBridge,
                      PriceQuoteService priceQuoteService,
                      SkuSpecRegistry skuSpecRegistry,
                      OrderValidationChain validationChain,
                      CommandInvoker commandInvoker,
                      DiscountStrategyRouter discountRouter,
                      OrderStateMachine stateMachine,
                      ReceiptService receiptService,
                      OrderMediator mediator,
                      SnapshotCareTaker snapshotCareTaker,
                      OrderEventListeners eventListeners,
                      OrderRecordRepository repository,
                      PlaceOrderFacade facade,
                      ServiceTimingAspect timingAspect) {
        this.rules = rules;
        this.payChannelFactory = payChannelFactory;
        this.channelKitFactory = channelKitFactory;
        this.templateRegistry = templateRegistry;
        this.cartDraftProvider = cartDraftProvider;
        this.logisticsRouter = logisticsRouter;
        this.notificationBridge = notificationBridge;
        this.priceQuoteService = priceQuoteService;
        this.skuSpecRegistry = skuSpecRegistry;
        this.validationChain = validationChain;
        this.commandInvoker = commandInvoker;
        this.discountRouter = discountRouter;
        this.stateMachine = stateMachine;
        this.receiptService = receiptService;
        this.mediator = mediator;
        this.snapshotCareTaker = snapshotCareTaker;
        this.eventListeners = eventListeners;
        this.repository = repository;
        this.facade = facade;
        this.timingAspect = timingAspect;
    }

    public List<String> keys() {
        return Arrays.asList("singleton", "factory-method", "abstract-factory", "builder", "prototype",
                "adapter", "bridge", "composite", "decorator", "facade", "flyweight", "proxy",
                "chain", "command", "interpreter", "iterator", "mediator", "memento", "observer",
                "state", "strategy", "template", "visitor");
    }

    public Object run(String key) {
        switch (key == null ? "" : key.toLowerCase(Locale.ROOT)) {
            case "singleton":
                return Map.of("rules", rules.snapshot(), "reloadCount", rules.reloadCount());
            case "factory-method": {
                PayReceipt receipt = payChannelFactory.pay("ALIPAY", "DEMO-1", new BigDecimal("88.00"));
                return Map.of("supported", payChannelFactory.supportedChannels(),
                        "channel", receipt.getChannel(), "serialNo", receipt.getSerialNo(),
                        "result", receipt.getResult());
            }
            case "abstract-factory": {
                ChannelKit kit = channelKitFactory.forChannel("CROSS_BORDER");
                return Map.of("orderNo", kit.newOrderNo(),
                        "settle(100)", kit.settle(new BigDecimal("100")).toPlainString(),
                        "note", kit.complianceNote());
            }
            case "builder": {
                PromotionRule rule = PromotionRule.builder()
                        .code("DEMO-RULE").name("满百减二十").type(PromotionRule.Type.FULL_REDUCTION)
                        .threshold(new BigDecimal("100")).value(new BigDecimal("20"))
                        .channel("DOMESTIC").priority(1).build();
                return rule.toString();
            }
            case "prototype": {
                PromotionTemplate activity = templateRegistry.newActivity("DOUBLE11", "演示活动");
                activity.addSku("SKU-X");
                return Map.of("activitySkus", activity.getSkuWhitelist(),
                        "templateSkusUnchanged", templateRegistry.inspect("DOUBLE11").getSkuWhitelist(),
                        "freshCartDraft", cartDraftProvider.newDraft().items());
            }
            case "adapter": {
                LogisticsClient.ShipmentTicket ticket = logisticsRouter.ship("SF", "DEMO-2", "上海市测试路 1 号");
                return Map.of("carriers", logisticsRouter.carriers(),
                        "waybillNo", ticket.getWaybillNo(), "orderNo", ticket.getOrderNo());
            }
            case "bridge":
                return notificationBridge.send("ALERT", "EMAIL", "ops@example.com", "库存低于阈值");
            case "composite": {
                PromotionRuleTree.EvalContext ctx = new PromotionRuleTree.EvalContext();
                ctx.setAmount(new BigDecimal("120"));
                ctx.setVip(true);
                ctx.setRiskScore(10);
                PromotionRuleTree.RuleNode node = PromotionRuleTree.couponGate();
                return Map.of("tree", node.describe(), "result", node.evaluate(ctx));
            }
            case "decorator": {
                priceQuoteService.quote("SKU-PHONE");
                priceQuoteService.quote("SKU-PHONE");
                return priceQuoteService.stats();
            }
            case "facade":
                return facade.overview();
            case "flyweight":
                skuSpecRegistry.intern("SKU-PHONE");
                skuSpecRegistry.intern("SKU-PHONE");
                return Map.of("spec", skuSpecRegistry.intern("SKU-PHONE").asMap(),
                        "poolSize", skuSpecRegistry.pooledSize());
            case "proxy": {
                List<String> audit = new ArrayList<>();
                JdkAuditProxy.StockService stock =
                        JdkAuditProxy.wrap(new JdkAuditProxy.DefaultStockService(), audit);
                stock.deduct("SKU-GIFT", 2);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("jdkDynamicProxyAudit", audit);
                out.put("left", stock.available("SKU-GIFT"));
                out.put("springAop", timingAspect.stats());
                return out;
            }
            case "chain": {
                OrderRequest request = sampleRequest();
                return Map.of("checkNames", validationChain.checkNames(),
                        "passed", validationChain.validate(request).isPassed(),
                        "passedChecks", validationChain.validate(request).getPassedChecks());
            }
            case "command": {
                String id = "DEMO-" + UUID.randomUUID();
                Map<String, Object> context = new HashMap<>();
                OrderCommand command = new OrderCommand() {
                    @Override
                    public String id() {
                        return id;
                    }

                    @Override
                    public int maxAttempts() {
                        return 2;
                    }

                    @Override
                    public String execute(Map<String, Object> ctx) {
                        return "演示命令执行成功";
                    }

                    @Override
                    public void undo(Map<String, Object> ctx) {
                        // 演示用：无持久副作用
                    }
                };
                return Map.of("first", commandInvoker.run(command, context).getStatus(),
                        "replay", commandInvoker.run(command, context).getStatus(),
                        "undo", commandInvoker.undoLast(context).getStatus());
            }
            case "interpreter": {
                PromotionExpression.Context ctx = new PromotionExpression.Context();
                ctx.setAmount(new BigDecimal("150"));
                ctx.setVip(true);
                ctx.setRiskScore(10);
                return Map.of("spel", PromotionExpression.Demo.SPEL,
                        "spelResult", PromotionExpression.evaluate(PromotionExpression.Demo.SPEL, ctx),
                        "handWrittenAstResult", PromotionExpression.demoAst().interpret(ctx));
            }
            case "iterator": {
                PagedOrderCursor cursor = new PagedOrderCursor(repository, 2);
                List<String> orderNos = new ArrayList<>();
                while (cursor.hasNext()) {
                    orderNos.add(cursor.next().getOrderNo());
                }
                return Map.of("batchSize", 2, "batchesRead", cursor.batchesRead(), "orderNos", orderNos);
            }
            case "mediator":
                return Map.of("colleagues", mediator.colleagueNames(),
                        "steps", mediator.paid("DEMO-3", new BigDecimal("99")));
            case "memento": {
                CartEditor editor = new CartEditor();
                editor.add("SKU-PHONE", 1, new BigDecimal("2999"));
                snapshotCareTaker.push(editor.save("改单前"));
                editor.clear();
                editor.add("SKU-GIFT", 1, new BigDecimal("9.9"));
                return Map.of("afterChange", editor.view(),
                        "snapshotLabels", snapshotCareTaker.labels());
            }
            case "observer":
                return eventListeners.trail();
            case "state":
                return Map.of("transitions", stateMachine.transitionTable(),
                        "createdAllows", stateMachine.allowed(OrderStatus.CREATED),
                        "createdAfterPay", stateMachine.fire(OrderStatus.CREATED, Event.PAY).name());
            case "strategy": {
                DiscountContext ctx = new DiscountContext();
                ctx.setOriginalAmount(new BigDecimal("200"));
                ctx.setVip(true);
                String best = discountRouter.bestType(ctx);
                return Map.of("types", discountRouter.types(), "best", best,
                        "discount", discountRouter.apply(best, ctx).toPlainString());
            }
            case "template": {
                Context ctx = new Context();
                ctx.setOrderNo("DEMO-4");
                ctx.setAmount(new BigDecimal("100"));
                ctx.setBuyer("tester");
                return Map.of("channels", receiptService.channels(),
                        "crossBorderTrail", receiptService.generate("CROSS_BORDER", ctx).getTrail(),
                        "domesticTrail", receiptService.generate("DOMESTIC", ctx).getTrail());
            }
            case "visitor":
                return OrderLineVisitorDemo.summarize(OrderLineVisitorDemo.sampleItems());
            default:
                Map<String, Object> unknown = new LinkedHashMap<>();
                unknown.put("error", "未知模式: " + key);
                unknown.put("available", keys());
                return unknown;
        }
    }

    private OrderRequest sampleRequest() {
        OrderRequest request = new OrderRequest();
        request.setBuyer("tester");
        request.setSku("SKU-PHONE");
        request.setUnitPrice(new BigDecimal("199"));
        request.setQuantity(1);
        request.setPayChannel("ALIPAY");
        request.setRequestId("DEMO-" + UUID.randomUUID());
        return request;
    }
}

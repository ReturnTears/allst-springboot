package com.allst.springboot.behavioral;

import com.allst.springboot.behavioral.chain.OrderValidationChain;
import com.allst.springboot.behavioral.command.CommandInvoker;
import com.allst.springboot.behavioral.command.OrderCommand;
import com.allst.springboot.behavioral.interpreter.PromotionExpression;
import com.allst.springboot.behavioral.iterator.PagedOrderCursor;
import com.allst.springboot.behavioral.mediator.OrderMediator;
import com.allst.springboot.behavioral.memento.CartSnapshot;
import com.allst.springboot.behavioral.state.OrderStateMachine;
import com.allst.springboot.behavioral.strategy.DiscountStrategyRouter;
import com.allst.springboot.behavioral.template.ReceiptService;
import com.allst.springboot.behavioral.template.ReceiptTemplate;
import com.allst.springboot.behavioral.visitor.OrderLineVisitorDemo;
import com.allst.springboot.common.BizException;
import com.allst.springboot.domain.OrderRecord;
import com.allst.springboot.domain.OrderRecordRepository;
import com.allst.springboot.domain.OrderRequest;
import com.allst.springboot.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BehavioralPatternsTest {

    @Autowired
    private OrderValidationChain validationChain;
    @Autowired
    private CommandInvoker commandInvoker;
    @Autowired
    private OrderMediator mediator;
    @Autowired
    private OrderStateMachine stateMachine;
    @Autowired
    private DiscountStrategyRouter discountRouter;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderRecordRepository repository;

    @Test
    @DisplayName("责任链：正常请求过全链，风控失败在第二关短路且不继续")
    void chain() {
        OrderRequest ok = request(10);
        OrderValidationChain.ValidationResult passed = validationChain.validate(ok);
        assertThat(passed.isPassed()).isTrue();
        assertThat(passed.getPassedChecks()).hasSize(5);
        assertThat(passed.getPassedChecks().get(0)).isEqualTo("参数校验");

        OrderRequest risky = request(95);
        OrderValidationChain.ValidationResult rejected = validationChain.validate(risky);
        assertThat(rejected.isPassed()).isFalse();
        assertThat(rejected.getFailedAt()).isEqualTo("风控校验");
        assertThat(rejected.getReason()).contains("风控分过高");
    }

    @Test
    @DisplayName("命令：幂等去重、失败重试、撤销")
    void command() {
        String id = "TEST-" + UUID.randomUUID();
        Map<String, Object> context = new HashMap<>();
        OrderCommand success = new OrderCommand() {
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
                return "done";
            }

            @Override
            public void undo(Map<String, Object> ctx) {
                ctx.put("undone", true);
            }
        };
        assertThat(commandInvoker.run(success, context).getStatus()).isEqualTo("OK");
        assertThat(commandInvoker.run(success, context).getStatus()).isEqualTo("DUPLICATE");
        assertThat(commandInvoker.undoLast(context).getStatus()).isEqualTo("OK");
        assertThat(context).containsEntry("undone", true);

        AtomicInteger attempts = new AtomicInteger();
        OrderCommand flaky = new OrderCommand() {
            @Override
            public String id() {
                return "FLAKY-" + UUID.randomUUID();
            }

            @Override
            public int maxAttempts() {
                return 3;
            }

            @Override
            public String execute(Map<String, Object> ctx) {
                if (attempts.incrementAndGet() < 3) {
                    throw new IllegalStateException("下游超时");
                }
                return "recovered";
            }

            @Override
            public void undo(Map<String, Object> ctx) {
                // 演示用
            }
        };
        CommandInvoker.CommandResult retried = commandInvoker.run(flaky, context);
        assertThat(retried.getStatus()).isEqualTo("OK");
        assertThat(retried.getAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("解释器：手写语法树与 SpEL 结果一致")
    void interpreter() {
        PromotionExpression.Context ctx = new PromotionExpression.Context();
        ctx.setAmount(new BigDecimal("150"));
        ctx.setVip(true);
        ctx.setRiskScore(10);
        assertThat(PromotionExpression.evaluate(PromotionExpression.Demo.SPEL, ctx)).isTrue();
        assertThat(PromotionExpression.demoAst().interpret(ctx)).isTrue();
        ctx.setVip(false);
        ctx.setNewUser(false);
        assertThat(PromotionExpression.evaluate(PromotionExpression.Demo.SPEL, ctx)).isFalse();
    }

    @Test
    @DisplayName("迭代器：按批扫描，不一次载入全部订单")
    void iterator() {
        List<String> seeded = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String no = "ITER-" + UUID.randomUUID();
            seeded.add(no);
            OrderRecord record = new OrderRecord();
            record.setOrderNo(no);
            record.setBuyer("tester");
            record.setSku("SKU-PHONE");
            record.setQuantity(1);
            record.setOriginalAmount(new BigDecimal("100"));
            record.setDiscount(BigDecimal.ZERO);
            record.setPayable(new BigDecimal("100"));
            record.setStatus(OrderStatus.PAID);
            record.setPayChannel("ALIPAY");
            record.setRequestId("ITER-REQ-" + UUID.randomUUID());
            record.setCreatedAt(Instant.now());
            repository.save(record);
        }
        PagedOrderCursor cursor = new PagedOrderCursor(repository, 2);
        List<String> scanned = new ArrayList<>();
        while (cursor.hasNext()) {
            scanned.add(cursor.next().getOrderNo());
        }
        assertThat(scanned).containsAll(seeded);
        assertThat(cursor.batchesRead()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("中介者：同事之间不互相引用，顺序由中介者定")
    void mediator() {
        List<String> steps = mediator.paid("M-1", new BigDecimal("99"));
        assertThat(steps).hasSize(4);
        assertThat(steps.get(0)).startsWith("库存:");
        assertThat(mediator.colleagueNames()).containsExactly("库存", "积分", "通知", "审计");
    }

    @Test
    @DisplayName("备忘录：快照是副本，改乱之后能还原")
    void memento() {
        CartSnapshot.CartEditor editor = new CartSnapshot.CartEditor();
        editor.add("SKU-PHONE", 1, new BigDecimal("2999"));
        CartSnapshot.Memento snapshot = editor.save("改前");
        editor.clear();
        editor.add("SKU-GIFT", 1, new BigDecimal("9.9"));
        assertThat(editor.view()).containsExactly("SKU-GIFT x1");
        editor.restore(snapshot);
        assertThat(editor.view()).containsExactly("SKU-PHONE x1");
        assertThat(snapshot.getTotal()).isEqualByComparingTo("2999");
    }

    @Test
    @DisplayName("状态：合法转移通过，非法转移被拒")
    void state() {
        assertThat(stateMachine.fire(OrderStatus.CREATED, OrderStateMachine.Event.PAY))
                .isEqualTo(OrderStatus.PAID);
        assertThat(stateMachine.allowed(OrderStatus.PAID)).containsExactly("SHIP", "CANCEL");
        assertThatThrownBy(() -> stateMachine.fire(OrderStatus.PAID, OrderStateMachine.Event.CLOSE))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("策略：同一订单自动选优惠最大的算法")
    void strategy() {
        DiscountStrategyRouter.DiscountContext ctx = new DiscountStrategyRouter.DiscountContext();
        ctx.setOriginalAmount(new BigDecimal("200"));
        ctx.setVip(true);
        assertThat(discountRouter.types()).contains("NONE", "FULL_REDUCTION", "VIP", "NEW_USER");
        assertThat(discountRouter.bestType(ctx)).isEqualTo("VIP");
        assertThat(discountRouter.apply("VIP", ctx)).isEqualByComparingTo("30.00");
        assertThat(discountRouter.apply("FULL_REDUCTION", ctx)).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("模板方法：骨架一致，跨境回执通过钩子补报关清单")
    void templateMethod() {
        ReceiptTemplate.Context ctx = new ReceiptTemplate.Context();
        ctx.setOrderNo("T-1");
        ctx.setBuyer("tester");
        ctx.setAmount(new BigDecimal("100"));
        assertThat(receiptService.generate("CROSS_BORDER", ctx).getTrail())
                .contains("附加报关清单");
        assertThat(receiptService.generate("DOMESTIC", ctx).getTrail())
                .doesNotContain("附加报关清单");
        assertThat(receiptService.generate("DOMESTIC", ctx).getTax()).isEqualByComparingTo("13.00");
    }

    @Test
    @DisplayName("访问者：换一套口径重算，礼品卡不计入营收")
    void visitor() {
        Map<String, Object> summary = OrderLineVisitorDemo.summarize(OrderLineVisitorDemo.sampleItems());
        assertThat(summary.get("recognisedAmount")).isEqualTo(new BigDecimal("3397"));
        assertThat((List<?>) summary.get("invoiceCategories")).hasSize(3);
    }

    private OrderRequest request(int riskScore) {
        OrderRequest request = new OrderRequest();
        request.setBuyer("tester");
        request.setSku("SKU-PHONE");
        request.setUnitPrice(new BigDecimal("199"));
        request.setQuantity(1);
        request.setPayChannel("ALIPAY");
        request.setRequestId("REQ-" + UUID.randomUUID());
        request.setRiskScore(riskScore);
        return request;
    }
}

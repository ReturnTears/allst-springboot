package com.allst.springboot.tradeorder;

import com.allst.springboot.common.async.AsyncTaskRecorder;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.AppClock;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderEventCommand;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.domain.SkuStock;
import com.allst.springboot.tradeorder.repo.SkuStockRepository;
import com.allst.springboot.tradeorder.service.PayTimeoutScanner;
import com.allst.springboot.tradeorder.service.TradeOrderAppService;
import com.allst.springboot.tradeorder.service.TradeOrderView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TradeOrderFlowTest {

    @Autowired
    private TradeOrderAppService appService;
    @Autowired
    private SkuStockRepository stockRepository;
    @Autowired
    private AsyncTaskRecorder recorder;
    @Autowired
    private AppClock clock;
    @Autowired
    private PayTimeoutScanner scanner;

    @AfterEach
    void unfreezeClock() {
        clock.unfreeze();
    }

    @Test
    @DisplayName("低风险订单沿主路径走到完成，仓库通知只发一次")
    void lowRiskOrderReachesCompleted() {
        upsertStock("SKU-PHONE", 20);
        TradeOrderView created = create("SKU-PHONE", 2, 10);
        assertThat(created.getAllowedEvents()).extracting("code").contains("SUBMIT_PAY", "CANCEL");

        TradeOrderView paying = fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, "pay-1", null);
        TradeOrderView allocated = fire(paying.getOrderNo(), OrderEvent.PAY_SUCCESS, "pay-ok", null);
        assertThat(allocated.getStatus()).isEqualTo(OrderState.WAIT_ALLOCATE);
        assertThat(allocated.getPaidAt()).isNotNull();

        TradeOrderView waitingShip = fire(allocated.getOrderNo(), OrderEvent.ALLOCATE, "alloc-1", null);
        assertThat(waitingShip.getStatus()).isEqualTo(OrderState.WAIT_SHIP);
        assertThat(stockRepository.findBySkuCode("SKU-PHONE").orElseThrow().getAvailable()).isEqualTo(18);
        awaitTask("warehouse:" + created.getOrderNo());

        TradeOrderView shipped = fire(waitingShip.getOrderNo(), OrderEvent.SHIP, "ship-1", "SF1001");
        assertThat(shipped.getStatus()).isEqualTo(OrderState.SHIPPED);
        assertThat(shipped.getShipmentNo()).isEqualTo("SF1001");
        assertThat(recorder.countDone("warehouse:" + created.getOrderNo())).isEqualTo(1);

        TradeOrderView delivered = fire(shipped.getOrderNo(), OrderEvent.SIGN, "sign-1", null);
        TradeOrderView completed = fire(delivered.getOrderNo(), OrderEvent.CONFIRM, "confirm-1", null);
        assertThat(completed.getStatus()).isEqualTo(OrderState.COMPLETED);
        assertThat(completed.getAllowedEvents()).isEmpty();
        awaitTask("completed:" + created.getOrderNo());
        assertThat(appService.logs(created.getOrderNo())).hasSize(6);
    }

    @Test
    @DisplayName("高风险支付进入人工审核，拒绝后走退款")
    void highRiskRejectThenRefund() {
        upsertStock("SKU-PHONE", 10);
        TradeOrderView created = create("SKU-PHONE", 1, 90);
        fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, "pay-hr", null);
        TradeOrderView review = fire(created.getOrderNo(), OrderEvent.PAY_SUCCESS, "pay-hr-ok", null);
        assertThat(review.getStatus()).isEqualTo(OrderState.RISK_REVIEW);

        TradeOrderView rejected = fire(created.getOrderNo(), OrderEvent.RISK_REJECT, "risk-no", null);
        assertThat(rejected.getStatus()).isEqualTo(OrderState.RISK_REJECTED);
        TradeOrderView refunding = fire(created.getOrderNo(), OrderEvent.START_REFUND, "refund-1", null);
        assertThat(refunding.getStatus()).isEqualTo(OrderState.REFUNDING);
        awaitTask("refund:" + created.getOrderNo());
        TradeOrderView refunded = fire(created.getOrderNo(), OrderEvent.REFUND_SUCCESS, "refund-ok", null);
        assertThat(refunded.getStatus()).isEqualTo(OrderState.REFUNDED);
    }

    @Test
    @DisplayName("待支付不能发货，重复事件号不会再转移")
    void illegalEventAndIdempotentReplay() {
        upsertStock("SKU-PHONE", 10);
        TradeOrderView created = create("SKU-PHONE", 1, 5);
        assertThatThrownBy(() -> fire(created.getOrderNo(), OrderEvent.SHIP, "ship-too-early", "SF1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能接受");

        TradeOrderView paying = fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, "same-event", null);
        TradeOrderView replay = fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, "same-event", null);
        assertThat(replay.isReplayed()).isTrue();
        assertThat(replay.getStatus()).isEqualTo(paying.getStatus());
        assertThat(appService.logs(created.getOrderNo())).hasSize(1);
    }

    @Test
    @DisplayName("库存不够进入缺货，补货后再次占用成功")
    void stockShortThenRetry() {
        upsertStock("SKU-GIFT", 2);
        TradeOrderView created = create("SKU-GIFT", 3, 1);
        fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, "pay-s", null);
        fire(created.getOrderNo(), OrderEvent.PAY_SUCCESS, "pay-s-ok", null);
        TradeOrderView shortStock = fire(created.getOrderNo(), OrderEvent.ALLOCATE, "alloc-short", null);
        assertThat(shortStock.getStatus()).isEqualTo(OrderState.STOCK_SHORT);
        assertThat(stockRepository.findBySkuCode("SKU-GIFT").orElseThrow().getAvailable()).isEqualTo(2);

        upsertStock("SKU-GIFT", 5);
        TradeOrderView waitingShip = fire(created.getOrderNo(), OrderEvent.ALLOCATE, "alloc-retry", null);
        assertThat(waitingShip.getStatus()).isEqualTo(OrderState.WAIT_SHIP);
        assertThat(stockRepository.findBySkuCode("SKU-GIFT").orElseThrow().getAvailable()).isEqualTo(2);
    }

    @Test
    @DisplayName("退货被拒绝后关闭售后，回到已签收再确认完成")
    void returnRejectedThenComplete() {
        upsertStock("SKU-PHONE", 8);
        String orderNo = payAndDeliver("return-reject");
        TradeOrderView applied = fire(orderNo, OrderEvent.APPLY_RETURN, "return-1", null);
        assertThat(applied.getStatus()).isEqualTo(OrderState.RETURN_APPLIED);
        TradeOrderView rejected = fire(orderNo, OrderEvent.REJECT_RETURN, "return-no", null);
        assertThat(rejected.getStatus()).isEqualTo(OrderState.RETURN_REJECTED);
        TradeOrderView delivered = fire(orderNo, OrderEvent.CLOSE_AFTER_SALE, "return-close", null);
        assertThat(delivered.getStatus()).isEqualTo(OrderState.DELIVERED);
        TradeOrderView completed = fire(orderNo, OrderEvent.CONFIRM, "confirm-back", null);
        assertThat(completed.getStatus()).isEqualTo(OrderState.COMPLETED);
    }

    @Test
    @DisplayName("同意退货后进入退款")
    void returnApprovedThenRefund() {
        upsertStock("SKU-PHONE", 8);
        String orderNo = payAndDeliver("return-ok");
        fire(orderNo, OrderEvent.APPLY_RETURN, "return-a", null);
        TradeOrderView approved = fire(orderNo, OrderEvent.APPROVE_RETURN, "return-yes", null);
        assertThat(approved.getStatus()).isEqualTo(OrderState.RETURN_APPROVED);
        TradeOrderView refunding = fire(orderNo, OrderEvent.START_REFUND, "return-refund", null);
        assertThat(refunding.getStatus()).isEqualTo(OrderState.REFUNDING);
        TradeOrderView refunded = fire(orderNo, OrderEvent.REFUND_SUCCESS, "return-refund-ok", null);
        assertThat(refunded.getStatus()).isEqualTo(OrderState.REFUNDED);
    }

    @Test
    @DisplayName("超过支付时限不能再支付，扫描器关闭订单")
    void payWindowAndScanner() {
        upsertStock("SKU-PHONE", 6);
        TradeOrderView created = create("SKU-PHONE", 1, 3);
        clock.freeze(LocalDateTime.now().plusMinutes(31));
        assertThatThrownBy(() -> fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, "pay-late", null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("支付时限");
        int closed = scanner.scan();
        assertThat(closed).isGreaterThanOrEqualTo(1);
        assertThat(appService.get(created.getOrderNo()).getStatus()).isEqualTo(OrderState.CLOSED);
    }

    @Test
    @DisplayName("两笔订单同时抢最后一件库存，只有一笔占用成功")
    void concurrentAllocateKeepsStockConsistent() throws Exception {
        upsertStock("SKU-RACE", 1);
        TradeOrderView first = create("SKU-RACE", 1, 1);
        TradeOrderView second = create("SKU-RACE", 1, 1);
        prepareWaitingAllocate(first.getOrderNo(), "race-a");
        prepareWaitingAllocate(second.getOrderNo(), "race-b");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<OrderState> states = new CopyOnWriteArrayList<>();
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        pool.submit(allocateAfter(start, first.getOrderNo(), "race-a-alloc", states, errors));
        pool.submit(allocateAfter(start, second.getOrderNo(), "race-b-alloc", states, errors));
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).isEmpty();
        assertThat(states).containsExactlyInAnyOrder(OrderState.WAIT_SHIP, OrderState.STOCK_SHORT);
        assertThat(stockRepository.findBySkuCode("SKU-RACE").orElseThrow().getAvailable()).isZero();
    }

    @Test
    @DisplayName("发货缺少物流单号会被守卫拒绝")
    void shipRequiresShipmentNo() {
        upsertStock("SKU-PHONE", 4);
        TradeOrderView created = create("SKU-PHONE", 1, 2);
        fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, "pay-ship", null);
        fire(created.getOrderNo(), OrderEvent.PAY_SUCCESS, "pay-ship-ok", null);
        fire(created.getOrderNo(), OrderEvent.ALLOCATE, "alloc-ship", null);
        assertThatThrownBy(() -> fire(created.getOrderNo(), OrderEvent.SHIP, "ship-blank", " "))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("物流单号");
    }

    private void prepareWaitingAllocate(String orderNo, String prefix) {
        fire(orderNo, OrderEvent.SUBMIT_PAY, prefix + "-pay", null);
        TradeOrderView paid = fire(orderNo, OrderEvent.PAY_SUCCESS, prefix + "-ok", null);
        assertThat(paid.getStatus()).isEqualTo(OrderState.WAIT_ALLOCATE);
    }

    private Runnable allocateAfter(CountDownLatch start, String orderNo, String eventId,
                                   List<OrderState> states, List<Throwable> errors) {
        return () -> {
            try {
                start.await(5, TimeUnit.SECONDS);
                states.add(fire(orderNo, OrderEvent.ALLOCATE, eventId, null).getStatus());
            } catch (Throwable throwable) {
                errors.add(throwable);
            }
        };
    }

    private String payAndDeliver(String prefix) {
        TradeOrderView created = create("SKU-PHONE", 1, 4);
        fire(created.getOrderNo(), OrderEvent.SUBMIT_PAY, prefix + "-pay", null);
        fire(created.getOrderNo(), OrderEvent.PAY_SUCCESS, prefix + "-ok", null);
        fire(created.getOrderNo(), OrderEvent.ALLOCATE, prefix + "-alloc", null);
        fire(created.getOrderNo(), OrderEvent.SHIP, prefix + "-ship", "SF-" + prefix);
        fire(created.getOrderNo(), OrderEvent.SIGN, prefix + "-sign", null);
        return created.getOrderNo();
    }

    private TradeOrderView create(String skuCode, int quantity, int riskScore) {
        return appService.create("buyer", skuCode, quantity, new BigDecimal("19.90"), riskScore);
    }

    private TradeOrderView fire(String orderNo, OrderEvent event, String eventId, String shipmentNo) {
        OrderEventCommand command = new OrderEventCommand();
        command.setEvent(event);
        command.setOperator("tester");
        command.setEventId(eventId);
        command.setShipmentNo(shipmentNo);
        return appService.fire(orderNo, command);
    }

    private void upsertStock(String skuCode, int available) {
        SkuStock stock = stockRepository.findBySkuCode(skuCode).orElseGet(SkuStock::new);
        stock.setSkuCode(skuCode);
        stock.setAvailable(available);
        stockRepository.saveAndFlush(stock);
    }

    private void awaitTask(String taskName) {
        long deadline = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < deadline) {
            if (recorder.countDone(taskName) > 0) {
                return;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(recorder.snapshot()).contains("done:" + taskName);
    }
}

package com.allst.springboot.tradeorder.machine.guard;

import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.machine.GuardChain;
import com.allst.springboot.tradeorder.service.StockService;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * 库存是否足够和占住库存必须是同一次条件更新。
 * 守卫返回 true 表示本次事务里已经扣减成功；返回 false 表示走缺货分支。
 * extended state 上的标记用来防止同一次事件里守卫被重复求值时扣两次。
 */
@Component
public class ReserveStockGuard implements Guard<OrderState, OrderEvent> {

    private final StockService stockService;

    public ReserveStockGuard(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public boolean evaluate(StateContext<OrderState, OrderEvent> context) {
        if (Boolean.TRUE.equals(GuardChain.variable(context, "stockReserved"))) {
            return true;
        }
        String skuCode = String.valueOf(GuardChain.variable(context, "skuCode"));
        Object quantityValue = GuardChain.variable(context, "quantity");
        int quantity = quantityValue instanceof Number ? ((Number) quantityValue).intValue() : 0;
        if (!stockService.exists(skuCode)) {
            throw new BizException("SKU 不存在: " + skuCode);
        }
        boolean reserved = stockService.tryDeduct(skuCode, quantity);
        if (reserved) {
            context.getExtendedState().getVariables().put("stockReserved", true);
        }
        return reserved;
    }
}

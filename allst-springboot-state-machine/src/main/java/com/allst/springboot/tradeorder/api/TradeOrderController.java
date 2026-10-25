package com.allst.springboot.tradeorder.api;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.tradeorder.domain.OrderEventCommand;
import com.allst.springboot.tradeorder.service.ParallelPrecheckService;
import com.allst.springboot.tradeorder.service.TradeOrderAppService;
import com.allst.springboot.tradeorder.service.TradeOrderView;
import com.allst.springboot.tradeorder.service.TransitionLogView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/trade-orders")
public class TradeOrderController {

    private final TradeOrderAppService appService;
    private final ParallelPrecheckService precheckService;

    public TradeOrderController(TradeOrderAppService appService, ParallelPrecheckService precheckService) {
        this.appService = appService;
        this.precheckService = precheckService;
    }

    @PostMapping
    public Result<TradeOrderView> create(@Valid @RequestBody CreateTradeOrderRequest request) {
        return Result.ok(appService.create(request.getBuyerName(), request.getSkuCode(), request.getQuantity(),
                request.getAmount(), request.getRiskScore()));
    }

    @PostMapping("/precheck")
    public Result<ParallelPrecheckService.PrecheckResult> precheck(@Valid @RequestBody PrecheckRequest request) {
        return Result.ok(precheckService.check(request.getSkuCode(), request.getQuantity(), request.getRiskScore()));
    }

    @GetMapping("/{orderNo}")
    public Result<TradeOrderView> get(@PathVariable String orderNo) {
        return Result.ok(appService.get(orderNo));
    }

    @GetMapping("/{orderNo}/logs")
    public Result<List<TransitionLogView>> logs(@PathVariable String orderNo) {
        return Result.ok(appService.logs(orderNo));
    }

    @PostMapping("/{orderNo}/events")
    public Result<TradeOrderView> fire(@PathVariable String orderNo, @Valid @RequestBody FireEventRequest request) {
        OrderEventCommand command = new OrderEventCommand();
        command.setEvent(request.getEvent());
        command.setOperator(request.getOperator());
        command.setEventId(request.getEventId());
        command.setRemark(request.getRemark());
        command.setShipmentNo(request.getShipmentNo());
        return Result.ok(appService.fire(orderNo, command));
    }
}

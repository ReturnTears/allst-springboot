package com.allst.springboot.controller;

import com.allst.springboot.annotation.Idempotent;
import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.api.Result;
import com.allst.springboot.common.header.Operators;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.common.header.RequestMeta;
import com.allst.springboot.domain.order.OrderBatchCreateCommand;
import com.allst.springboot.domain.order.OrderCommandService;
import com.allst.springboot.domain.order.OrderCreateCommand;
import com.allst.springboot.domain.order.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 订单写入。取消使用 PATCH，不和创建混在一个接口里。
 */
@Validated
@RestController
@RequireSign
@RequestMapping("/api/v1/orders")
@Tag(name = "订单写入")
public class OrderCommandController {

    private final OrderCommandService orderCommandService;

    public OrderCommandController(OrderCommandService orderCommandService) {
        this.orderCommandService = orderCommandService;
    }

    @PostMapping
    @Idempotent
    @Operation(summary = "创建订单")
    public ResponseEntity<Result<OrderVO>> create(@Valid @RequestBody OrderCreateCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.ok(orderCommandService.create(command, operator())));
    }

    @PostMapping("/batch")
    @Idempotent
    @Operation(summary = "批量创建订单")
    public ResponseEntity<Result<List<Long>>> batchCreate(@Valid @RequestBody OrderBatchCreateCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.ok(orderCommandService.batchCreate(command, operator())));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "只做取消。重复调用返回当前已取消的订单。")
    public Result<OrderVO> cancel(@PathVariable Long id) {
        return Result.ok(orderCommandService.cancel(id));
    }

    private String operator() {
        RequestMeta meta = RequestContext.get();
        return Operators.normalize(meta == null ? null : meta.getOperator());
    }
}

package com.allst.springboot.controller;

import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.api.PageData;
import com.allst.springboot.common.api.Result;
import com.allst.springboot.domain.order.OrderPageQuery;
import com.allst.springboot.domain.order.OrderQueryService;
import com.allst.springboot.domain.order.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequireSign
@RequestMapping("/api/v1/orders")
@Tag(name = "订单查询")
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    public OrderQueryController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    @Operation(summary = "分页查询订单")
    public Result<PageData<OrderVO>> page(@Valid OrderPageQuery query) {
        return Result.ok(orderQueryService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "按编号查询订单")
    public Result<OrderVO> get(@PathVariable Long id) {
        return Result.ok(orderQueryService.get(id));
    }
}

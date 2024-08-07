package com.allst.boot.controller;

import com.allst.boot.enums.CustomerEnum;
import com.allst.boot.model.OrderBO;
import com.allst.boot.model.OrderDiscount;
import com.allst.boot.service.OrderDiscountService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-08-07 下午 10:13
 */
@RestController
@RequestMapping("/order/discount")
public class OrderDiscountController {
    @Autowired
    private OrderDiscountService orderDiscountService;

    @GetMapping("/get")
    public String getDiscount() {
        OrderBO orderBO = new OrderBO();
        orderBO.setAge(20);
        orderBO.setAmount(20000);
        orderBO.setCustomerNumber("123456");
        orderBO.setCustomerEnum(CustomerEnum.LOYAL);
        OrderDiscount discount = orderDiscountService.getDiscount(orderBO);
        return new Gson().toJson(discount);
    }
}

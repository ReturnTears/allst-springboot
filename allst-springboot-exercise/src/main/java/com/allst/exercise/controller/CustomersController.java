package com.allst.exercise.controller;

import com.allst.exercise.model.Customers;
import com.allst.exercise.service.CustomersService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-07-27 下午 11:50
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomersController {

    private final CustomersService customersService;

    @GetMapping("/save")
    public Object save(Customers customers) {
        return customersService.save(customers);
    }
}

package com.allst.exercise.controller;

import com.allst.exercise.model.Account;
import com.allst.exercise.service.AccountService;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2023-08-13 下午 09:47
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Api(tags = "Account控制器")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/id/{id}")
    public String getOne(@PathVariable(value = "id") Long id) {
        Account accountOne = accountService.getAccountOne(id);
        return new Gson().toJson(accountOne);
    }
}

package com.allst.exercise.controller;

import com.allst.exercise.common.Result;
import com.allst.exercise.model.Account;
import com.allst.exercise.service.AccountService;
import io.swagger.annotations.Api;
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
@Api(tags = "Account控制器")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/id/{id}")
    public Result<Account> getOne(@PathVariable(value = "id") Long id) {
        return Result.ok(accountService.getAccountOne(id));
    }
}

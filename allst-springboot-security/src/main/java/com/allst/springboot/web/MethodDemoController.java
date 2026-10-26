package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.method.MethodDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 方法级安全的入口。链上只要求已登录，细粒度全在 {@code MethodDemoService}。
 * 控制器自己不要写权限判断，否则和注解各说各的，以后会改漏一处。
 */
@RestController
@RequestMapping("/api/method")
public class MethodDemoController {

    private final MethodDemoService methods;

    public MethodDemoController(MethodDemoService methods) {
        this.methods = methods;
    }

    @GetMapping("/admin")
    public ApiResult<String> admin() {
        return ApiResult.ok(methods.adminOnly());
    }

    @GetMapping("/read")
    public ApiResult<String> read() {
        return ApiResult.ok(methods.readUsers());
    }

    @GetMapping("/user/{id}")
    public ApiResult<String> user(@PathVariable Long id) {
        return ApiResult.ok(methods.readUser(id));
    }

    @GetMapping("/owner")
    public ApiResult<String> owner(@RequestParam String username) {
        return ApiResult.ok(methods.ownerName(username));
    }

    @PostMapping("/filter")
    public ApiResult<List<String>> filter(@RequestBody List<String> names) {
        return ApiResult.ok(methods.filterNames(names));
    }

    @GetMapping("/secured-auditor")
    public ApiResult<String> securedAuditor() {
        return ApiResult.ok(methods.securedAuditor());
    }

    @GetMapping("/jsr250-user")
    public ApiResult<String> jsr250User() {
        return ApiResult.ok(methods.jsr250User());
    }
}

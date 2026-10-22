package com.allst.springboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 保留的公开探测接口，不要求登录。安全过滤链对它单独放行，见 WebSecurityConfig。
 */
@RestController
@RequestMapping("/hello")
public class HelloController {
    @GetMapping(value = "/{name}")
    public String showMsg(@PathVariable(value = "name") String name) {
        return "Hello " + name;
    }
}
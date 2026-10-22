package com.allst.springboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 保留的示例接口，不走业务规范链路。规范接口从 /api 开始，说明见 docs/README.md。
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping(value = "/{msg}")
    public String showMsg(@PathVariable(value = "msg") String msg) {
        return "Hello " + msg;
    }

}

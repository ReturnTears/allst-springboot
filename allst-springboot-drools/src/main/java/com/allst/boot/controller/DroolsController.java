package com.allst.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-08-07 下午 09:46
 */
@RestController
@RequestMapping("/drools")
public class DroolsController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello Drools";
    }
}

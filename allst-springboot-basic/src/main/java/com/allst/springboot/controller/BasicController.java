package com.allst.springboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YiYa
 * @since 2020-08-04 下午 10:59
 */
@RestController
public class BasicController {

    @GetMapping(value = "/basic")
    public String basic() {
        return "springboot devtools basic";
    }
}

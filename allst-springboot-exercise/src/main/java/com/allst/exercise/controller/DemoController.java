package com.allst.exercise.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2023-08-13 下午 09:47
 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    @GetMapping("/doDemo")
    public String doDemo() {
        return "Hello doDemo";
    }
}

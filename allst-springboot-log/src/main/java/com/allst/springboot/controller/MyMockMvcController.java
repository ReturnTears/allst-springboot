package com.allst.springboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author June
 * @since 2021年06月
 */
@RestController
@RequestMapping("/myMM")
public class MyMockMvcController {

    @GetMapping("/str")
    public String toMyMMString() {
        return "Hello World";
    }
}

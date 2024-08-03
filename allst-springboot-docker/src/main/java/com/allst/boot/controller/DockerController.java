package com.allst.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-08-03 下午 10:11
 */
@RestController
@RequestMapping("/docker")
public class DockerController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello Docker!";
    }
}

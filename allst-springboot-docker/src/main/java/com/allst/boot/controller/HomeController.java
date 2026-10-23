package com.allst.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", "Hello Docker!");
        body.put("hello", "/docker/hello");
        body.put("info", "/docker/info");
        body.put("health", "/actuator/health");
        return body;
    }
}

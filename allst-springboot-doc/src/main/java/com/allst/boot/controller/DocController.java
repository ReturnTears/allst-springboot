package com.allst.boot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-07-27 上午 10:02
 */
@RestController
public class DocController {
    @RequestMapping("/doc")
    public String doc() {
        return "DocController";
    }
}

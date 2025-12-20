package com.allst.boot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2025-12-20 下午 07:40
 */
@RestController
@RequestMapping("/sa-token")
public class SatokenController {
    @GetMapping("/halo")
    public ResponseEntity<String> sayHalo() {
        return ResponseEntity.ok("Halo ~");
    }
}

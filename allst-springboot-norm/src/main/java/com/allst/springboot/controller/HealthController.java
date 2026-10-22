package com.allst.springboot.controller;

import com.allst.springboot.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探活和压测目标。不签名、不落访问日志，避免测量被安全链路淹没。
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "健康检查")
public class HealthController {

    @GetMapping
    @Operation(summary = "健康检查")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("app", "allst-springboot-norm");
        data.put("time", LocalDateTime.now().toString());
        return Result.ok(data);
    }
}

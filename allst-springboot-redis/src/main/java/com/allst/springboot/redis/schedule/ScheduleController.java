package com.allst.springboot.redis.schedule;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final DelayTaskService delayTaskService;

    public ScheduleController(DelayTaskService delayTaskService) {
        this.delayTaskService = delayTaskService;
    }

    @PostMapping("/tasks")
    public ApiResult<?> submit(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(delayTaskService.submit(
                Req.str(body, "type"),
                Req.optional(body, "payload", ""),
                Req.integer(body, "delaySeconds", 5)));
    }

    @GetMapping("/tasks")
    public ApiResult<?> pending() {
        return ApiResult.ok(delayTaskService.pending());
    }

    @GetMapping("/results")
    public ApiResult<?> results() {
        return ApiResult.ok(delayTaskService.results());
    }
}

package com.allst.springboot.redis.analytics;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/hit")
    public ApiResult<?> hit(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(analyticsService.hit(Req.str(body, "userId"), Req.str(body, "page")));
    }

    @GetMapping("/summary")
    public ApiResult<?> summary(@RequestParam(required = false) String day) {
        return ApiResult.ok(analyticsService.summary(day));
    }
}

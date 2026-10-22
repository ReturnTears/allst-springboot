package com.allst.springboot.redis.timeseries;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/timeseries")
public class TimeSeriesController {

    private final TimeSeriesService timeSeriesService;

    public TimeSeriesController(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    @PostMapping("/{metric}")
    public ApiResult<?> add(@PathVariable String metric, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(timeSeriesService.add(metric, Req.doubleValue(body, "value"), Req.longValue(body, "timestamp", 0)));
    }

    @GetMapping("/{metric}")
    public ApiResult<?> query(@PathVariable String metric,
                              @RequestParam(defaultValue = "0") long from,
                              @RequestParam(defaultValue = "0") long to,
                              @RequestParam(defaultValue = "0") int bucketSeconds) {
        return ApiResult.ok(timeSeriesService.query(metric, from, to, bucketSeconds));
    }

    @PostMapping("/{metric}/retain")
    public ApiResult<?> retain(@PathVariable String metric, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(timeSeriesService.retain(metric, Req.longValue(body, "before")));
    }
}

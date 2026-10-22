package com.allst.springboot.redis.mq;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mq")
public class MqController {

    private final MqService mqService;

    public MqController(MqService mqService) {
        this.mqService = mqService;
    }

    @PostMapping("/list")
    public ApiResult<?> list(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(mqService.enqueueList(Req.str(body, "to"), Req.str(body, "subject"), Req.str(body, "body")));
    }

    @GetMapping("/list")
    public ApiResult<?> listSnapshot() {
        return ApiResult.ok(mqService.listSnapshot());
    }

    @PostMapping("/stream")
    public ApiResult<?> stream(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(mqService.enqueueStream(Req.str(body, "to"), Req.str(body, "subject"), Req.str(body, "body")));
    }

    @GetMapping("/stream")
    public ApiResult<?> streamSnapshot() {
        return ApiResult.ok(mqService.streamSnapshot());
    }
}

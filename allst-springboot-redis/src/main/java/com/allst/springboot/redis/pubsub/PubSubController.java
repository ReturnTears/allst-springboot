package com.allst.springboot.redis.pubsub;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/pubsub")
public class PubSubController {

    private final NotifyHub notifyHub;

    public PubSubController(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    @PostMapping("/publish")
    public ApiResult<?> publish(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(notifyHub.publish(Req.str(body, "channel"), Req.str(body, "message")));
    }

    @GetMapping("/recent")
    public ApiResult<?> recent() {
        return ApiResult.ok(notifyHub.recent());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notifyHub.subscribe();
    }
}

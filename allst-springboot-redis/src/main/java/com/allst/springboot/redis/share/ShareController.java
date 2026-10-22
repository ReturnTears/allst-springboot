package com.allst.springboot.redis.share;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/config")
    public ApiResult<?> put(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(shareService.put(Req.str(body, "field"), Req.str(body, "value")));
    }

    @GetMapping("/config")
    public ApiResult<?> config() {
        return ApiResult.ok(shareService.snapshot());
    }

    @PostMapping("/heartbeat")
    public ApiResult<?> heartbeat(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(shareService.heartbeat(Req.str(body, "service"), Req.str(body, "instance")));
    }

    @GetMapping("/services")
    public ApiResult<?> services() {
        return ApiResult.ok(shareService.services());
    }
}

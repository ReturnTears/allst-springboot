package com.allst.springboot.controller;

import com.allst.springboot.annotation.RateLimit;
import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 把限流阈值收得很小，方便手工打出 429。业务接口按自己的注解单独配置。
 * HTTP 状态码 429 Too Many Requests 并非临时性故障，而是服务端主动实施的速率控制策略信号 --
 * 它表明客户端在指定时间窗口内发起的请求已超出预设配额，触发了服务端的限流熔断机制。理解其本质需穿透表层响应，
 * 深入协议语义、服务架构与可观测性协同层面。
 */
@RestController
@RequireSign
@RequestMapping("/api/v1/demo")
@Tag(name = "限流示例")
public class RateLimitDemoController {

    @GetMapping("/ping")
    @RateLimit(permits = 5, periodSeconds = 60)
    @Operation(summary = "限流探测", description = "同一 IP 在 60 秒内最多成功 5 次。")
    public Result<String> ping() {
        return Result.ok("pong");
    }
}

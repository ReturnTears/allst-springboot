package com.allst.springboot.redis.social;

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
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping("/follow")
    public ApiResult<?> follow(@RequestBody Map<String, Object> body) {
        boolean follow = !"false".equalsIgnoreCase(Req.optional(body, "follow", "true"));
        return ApiResult.ok(socialService.follow(Req.str(body, "user"), Req.str(body, "target"), follow));
    }

    @GetMapping("/mutual")
    public ApiResult<?> mutual(@RequestParam String a, @RequestParam String b) {
        return ApiResult.ok(socialService.mutual(a, b));
    }

    @PostMapping("/posts")
    public ApiResult<?> post(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(socialService.post(Req.str(body, "user"), Req.str(body, "content")));
    }

    @GetMapping("/feed")
    public ApiResult<?> feed(@RequestParam String user) {
        return ApiResult.ok(socialService.feed(user));
    }

    @PostMapping("/online")
    public ApiResult<?> online(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(socialService.online(Req.str(body, "user")));
    }

    @GetMapping("/online")
    public ApiResult<?> presence(@RequestParam String user) {
        return ApiResult.ok(socialService.presence(user));
    }

    @GetMapping("/profile")
    public ApiResult<?> profile(@RequestParam String user) {
        return ApiResult.ok(socialService.profile(user));
    }

    @PostMapping("/seed")
    public ApiResult<?> seed() {
        socialService.seed();
        return ApiResult.ok(socialService.profile("ada"));
    }
}

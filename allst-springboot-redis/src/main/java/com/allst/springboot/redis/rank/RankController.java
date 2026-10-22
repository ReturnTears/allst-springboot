package com.allst.springboot.redis.rank;

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
@RequestMapping("/api/rank")
public class RankController {

    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    @PostMapping("/score")
    public ApiResult<?> score(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(rankService.addScore(Req.str(body, "user"), Req.doubleValue(body, "delta")));
    }

    @GetMapping("/top")
    public ApiResult<?> top(@RequestParam(defaultValue = "10") int limit) {
        return ApiResult.ok(rankService.top(limit));
    }

    @GetMapping("/me")
    public ApiResult<?> me(@RequestParam String user) {
        return ApiResult.ok(rankService.me(user));
    }

    @PostMapping("/seed")
    public ApiResult<?> seed() {
        rankService.seed();
        return ApiResult.ok(rankService.top(10));
    }

    @PostMapping("/articles/{id}/view")
    public ApiResult<?> view(@PathVariable long id) {
        return ApiResult.ok(rankService.view(id));
    }

    @PostMapping("/articles/{id}/like")
    public ApiResult<?> like(@PathVariable long id, @RequestBody Map<String, Object> body) {
        boolean liked = !"false".equalsIgnoreCase(Req.optional(body, "liked", "true"));
        return ApiResult.ok(rankService.like(id, Req.str(body, "user"), liked));
    }

    @GetMapping("/articles/{id}")
    public ApiResult<?> counter(@PathVariable long id) {
        return ApiResult.ok(rankService.counter(id));
    }
}

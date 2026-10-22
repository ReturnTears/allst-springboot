package com.allst.springboot.redis.recommend;

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
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping("/behavior")
    public ApiResult<?> behavior(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(recommendService.record(Req.str(body, "userId"), Req.longValue(body, "itemId"), Req.str(body, "action")));
    }

    @GetMapping
    public ApiResult<?> recommend(@RequestParam String userId, @RequestParam(defaultValue = "5") int size) {
        return ApiResult.ok(recommendService.recommend(userId, size));
    }

    @GetMapping("/behaviors")
    public ApiResult<?> behaviors(@RequestParam String userId) {
        return ApiResult.ok(recommendService.latest(userId));
    }

    @PostMapping("/seed")
    public ApiResult<?> seed() {
        recommendService.seed();
        recommendService.record("qiu", 1L, "view");
        return ApiResult.ok(recommendService.recommend("qiu", 5));
    }
}

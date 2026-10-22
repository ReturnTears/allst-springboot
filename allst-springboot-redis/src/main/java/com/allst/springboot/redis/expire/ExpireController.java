package com.allst.springboot.redis.expire;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/expire")
public class ExpireController {

    private final ExpireService expireService;

    public ExpireController(ExpireService expireService) {
        this.expireService = expireService;
    }

    @PostMapping
    public ApiResult<?> put(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(expireService.put(Req.str(body, "key"), Req.str(body, "value"), Req.integer(body, "ttlSeconds")));
    }

    @GetMapping("/{key}")
    public ApiResult<?> inspect(@PathVariable String key) {
        return ApiResult.ok(expireService.inspect(key));
    }

    @PostMapping("/{key}/persist")
    public ApiResult<?> persist(@PathVariable String key) {
        return ApiResult.ok(expireService.persist(key));
    }

    @GetMapping("/log")
    public ApiResult<?> events() {
        return ApiResult.ok(expireService.events());
    }
}

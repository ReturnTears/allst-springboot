package com.allst.springboot.redis.session;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public ApiResult<?> login(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(sessionService.login(Req.str(body, "username")));
    }

    @GetMapping("/me")
    public ApiResult<?> me(@RequestHeader("X-Token") String token) {
        return ApiResult.ok(sessionService.me(token));
    }

    @PostMapping("/logout")
    public ApiResult<?> logout(@RequestHeader("X-Token") String token) {
        sessionService.logout(token);
        return ApiResult.ok("已退出");
    }

    @GetMapping("/cart")
    public ApiResult<?> cart(@RequestHeader("X-Token") String token) {
        return ApiResult.ok(sessionService.cart(token));
    }

    @PostMapping("/cart")
    public ApiResult<?> addCart(@RequestHeader("X-Token") String token, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(sessionService.addCart(token, Req.str(body, "skuId"), Req.integer(body, "qty")));
    }

    @DeleteMapping("/cart/{skuId}")
    public ApiResult<?> remove(@RequestHeader("X-Token") String token, @PathVariable String skuId) {
        return ApiResult.ok(sessionService.removeCartItem(token, skuId));
    }
}

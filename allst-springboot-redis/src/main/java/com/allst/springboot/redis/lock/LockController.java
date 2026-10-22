package com.allst.springboot.redis.lock;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lock")
public class LockController {

    private final StockLockService stockLockService;

    public LockController(StockLockService stockLockService) {
        this.stockLockService = stockLockService;
    }

    @PostMapping("/stock/deduct")
    public ApiResult<?> deduct(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(stockLockService.deduct(Req.longValue(body, "productId"), Req.integer(body, "qty")));
    }

    @PostMapping("/manual/try")
    public ApiResult<?> tryLock(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(stockLockService.tryManual(Req.str(body, "key"), Req.integer(body, "ttlSeconds", 10)));
    }

    @PostMapping("/manual/unlock")
    public ApiResult<?> unlock(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(stockLockService.unlockManual(Req.str(body, "key"), Req.str(body, "token")));
    }
}

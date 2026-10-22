package com.allst.springboot.redis.durable;

import com.allst.springboot.redis.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/durable")
public class PersistenceController {

    private final PersistenceService persistenceService;

    public PersistenceController(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @GetMapping
    public ApiResult<?> info() {
        return ApiResult.ok(persistenceService.info());
    }
}

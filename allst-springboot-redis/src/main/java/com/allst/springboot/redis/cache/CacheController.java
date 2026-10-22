package com.allst.springboot.redis.cache;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.Req;
import com.allst.springboot.redis.persistence.Product;
import com.allst.springboot.redis.persistence.ProductMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheAsideService cacheAsideService;
    private final ProductMapper productMapper;

    public CacheController(CacheAsideService cacheAsideService, ProductMapper productMapper) {
        this.cacheAsideService = cacheAsideService;
        this.productMapper = productMapper;
    }

    @GetMapping("/products/{id}")
    public ApiResult<?> product(@PathVariable long id) {
        return ApiResult.ok(cacheAsideService.getProduct(id, 0));
    }

    @PutMapping("/products/{id}")
    public ApiResult<?> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Product current = productMapper.findById(id);
        if (current == null) {
            throw new BizException("商品不存在");
        }
        current.setName(Req.str(body, "name"));
        current.setPrice(Req.decimal(body, "price"));
        current.setStock(Req.integer(body, "stock"));
        current.setDetail(Req.str(body, "detail"));
        current.setCategory(Req.str(body, "category"));
        return ApiResult.ok(cacheAsideService.update(current));
    }

    @GetMapping("/stats")
    public ApiResult<?> stats() {
        return ApiResult.ok(cacheAsideService.stats());
    }

    @PostMapping("/bloom/rebuild")
    public ApiResult<?> rebuild() {
        cacheAsideService.rebuildBloom();
        return ApiResult.ok(cacheAsideService.stats());
    }

    @GetMapping("/categories")
    public ApiResult<?> categories() {
        return ApiResult.ok(cacheAsideService.categories());
    }

    @PostMapping("/categories/evict")
    public ApiResult<?> evictCategories() {
        cacheAsideService.evictCategories();
        return ApiResult.ok("已删除字典缓存");
    }
}

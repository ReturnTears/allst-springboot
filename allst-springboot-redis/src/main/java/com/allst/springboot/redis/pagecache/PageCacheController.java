package com.allst.springboot.redis.pagecache;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/page")
public class PageCacheController {

    private final PageCacheService pageCacheService;

    public PageCacheController(PageCacheService pageCacheService) {
        this.pageCacheService = pageCacheService;
    }

    @GetMapping("/{slug}/view")
    public ApiResult<?> view(@PathVariable String slug) {
        return ApiResult.ok(pageCacheService.view(slug));
    }

    @GetMapping(value = "/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String html(@PathVariable String slug, HttpServletResponse response) {
        Map<String, Object> view = pageCacheService.view(slug);
        response.setHeader("X-Cache", Boolean.TRUE.equals(view.get("hit")) ? "HIT" : "MISS");
        response.setCharacterEncoding("UTF-8");
        return String.valueOf(view.get("html"));
    }

    @PutMapping("/{slug}")
    public ApiResult<?> update(@PathVariable String slug, @RequestBody Map<String, Object> body) {
        pageCacheService.update(slug, Req.str(body, "title"), Req.str(body, "content"));
        return ApiResult.ok("已更新并删除页面缓存");
    }

    @DeleteMapping("/{slug}/cache")
    public ApiResult<?> evict(@PathVariable String slug) {
        pageCacheService.evict(slug);
        return ApiResult.ok("已删除页面缓存");
    }
}

package com.allst.springboot.redis.pagecache;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.HtmlEscaper;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.persistence.PageContent;
import com.allst.springboot.redis.persistence.PageContentMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 整页缓存存的是渲染后的 HTML，不是数据库行。
 * 更新正文时删除缓存，下一次访问再渲染。不要在更新时直接写新 HTML，避免和并发渲染互相覆盖。
 * 对应文档 docs/10-page.md。
 */
@Service
public class PageCacheService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final PageContentMapper pageContentMapper;

    public PageCacheService(StringRedisTemplate redis, PageContentMapper pageContentMapper) {
        this.redis = redis;
        this.pageContentMapper = pageContentMapper;
    }

    public Map<String, Object> view(String slug) {
        String safeSlug = KeyGuard.slug(slug);
        String key = RedisKeys.pageHtml(safeSlug);
        String cached = redis.opsForValue().get(key);
        Map<String, Object> body = new LinkedHashMap<>();
        if (cached != null) {
            body.put("hit", true);
            body.put("ttlSeconds", redis.getExpire(key));
            body.put("html", cached);
            return body;
        }
        body.put("hit", false);
        body.put("ttlSeconds", TTL.getSeconds());
        body.put("html", loadAndCache(safeSlug));
        return body;
    }

    public void update(String slug, String title, String content) {
        String safeSlug = KeyGuard.slug(slug);
        if (title == null || title.isBlank() || title.length() > 80) {
            throw new BizException("标题长度需在 1 到 80 之间");
        }
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new BizException("正文长度需在 1 到 2000 之间");
        }
        PageContent page = pageContentMapper.findBySlug(safeSlug);
        if (page == null) {
            throw new BizException("页面不存在");
        }
        page.setTitle(title.trim());
        page.setContent(content.trim());
        pageContentMapper.update(page);
        redis.delete(RedisKeys.pageHtml(safeSlug));
    }

    public void evict(String slug) {
        redis.delete(RedisKeys.pageHtml(KeyGuard.slug(slug)));
    }

    private String loadAndCache(String slug) {
        PageContent page = pageContentMapper.findBySlug(slug);
        if (page == null) {
            throw new BizException("页面不存在");
        }
        String html = render(page);
        redis.opsForValue().set(RedisKeys.pageHtml(slug), html, TTL);
        return html;
    }

    private String render(PageContent page) {
        String[] lines = page.getContent().split("\\n");
        StringBuilder paragraphs = new StringBuilder();
        for (String line : lines) {
            if (!line.isBlank()) {
                paragraphs.append("<p>").append(HtmlEscaper.text(line)).append("</p>");
            }
        }
        return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><title>"
                + HtmlEscaper.text(page.getTitle())
                + "</title><style>body{margin:0;background:#f6f3ef;color:#1c1917;font-family:Georgia,serif}"
                + "main{max-width:680px;margin:48px auto;padding:32px;background:#fff;border:1px solid #e7e5e4}"
                + "h1{font-weight:500}p{line-height:1.7}</style></head><body><main><h1>"
                + HtmlEscaper.text(page.getTitle())
                + "</h1>"
                + paragraphs
                + "<p style=\"color:#78716c\">这一份 HTML 来自全页缓存示例。</p></main></body></html>";
    }
}

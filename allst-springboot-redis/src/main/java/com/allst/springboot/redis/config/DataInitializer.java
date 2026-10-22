package com.allst.springboot.redis.config;

import com.allst.springboot.redis.cache.CacheAsideService;
import com.allst.springboot.redis.persistence.PageContent;
import com.allst.springboot.redis.persistence.PageContentMapper;
import com.allst.springboot.redis.persistence.Product;
import com.allst.springboot.redis.persistence.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 只在表为空时写入示例商品和页面，避免每次重启覆盖你在控制台里改过的库存和正文。
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final ProductMapper productMapper;
    private final PageContentMapper pageContentMapper;
    private final CacheAsideService cacheAsideService;

    public DataInitializer(ProductMapper productMapper,
                           PageContentMapper pageContentMapper,
                           CacheAsideService cacheAsideService) {
        this.productMapper = productMapper;
        this.pageContentMapper = pageContentMapper;
        this.cacheAsideService = cacheAsideService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productMapper.count() == 0) {
            insert(1L, "机械键盘", "399.00", 20, "外设", "热插拔轴体，适合长时间编码。");
            insert(2L, "27 寸显示器", "1499.00", 8, "外设", "2K 分辨率，出厂校色。");
            insert(3L, "无线鼠标", "129.00", 50, "外设", "轻量化，续航 60 天。");
            insert(4L, "Redis 设计与实现", "88.00", 100, "图书", "用来对照本项目里的数据结构示例。");
            insert(5L, "降噪耳机", "899.00", 15, "音频", "通勤和开会都能用。");
            insert(6L, "移动固态硬盘", "599.00", 12, "存储", "读写速度适合拷贝项目镜像。");
            log.info("已写入 6 条示例商品");
        }
        if (pageContentMapper.count() == 0) {
            PageContent page = new PageContent();
            page.setId(1L);
            page.setSlug("home");
            page.setTitle("Redis 规范示例站");
            page.setContent("这一页的 HTML 会整页放进 Redis。\n更新正文后缓存会被删掉，下一次访问重新渲染。\n适合活动页、帮助中心这类读多写少的页面。");
            pageContentMapper.insert(page);
            log.info("已写入示例页面 home");
        }
        try {
            cacheAsideService.rebuildBloom();
        } catch (Exception ex) {
            log.warn("Redis 不可用，跳过布隆过滤器初始化: {}", ex.getMessage());
        }
    }

    private void insert(Long id, String name, String price, int stock, String category, String detail) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setDetail(detail);
        product.setCategory(category);
        productMapper.insert(product);
    }
}

package com.allst.springboot.redis.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制台侧栏和文档目录共用这份清单，避免前后端各写一份标题。
 */
public final class DemoCatalog {

    private DemoCatalog() {
    }

    public static List<Map<String, String>> items() {
        return Arrays.asList(
                item("structure", "00 数据结构", "String、Hash、List、Set、ZSet、Geo、HyperLogLog、Bitmap", "docs/00-structure.md"),
                item("cache", "01 缓存", "Cache-Aside、空值、互斥重建、随机过期、布隆过滤器", "docs/01-cache.md"),
                item("session", "02 会话存储", "登录态与购物车分离，滑动过期", "docs/02-session.md"),
                item("rank", "03 排行榜和计数器", "ZSET 实时排名，INCR 与 SET 去重点赞", "docs/03-rank.md"),
                item("mq", "04 消息队列", "List 可靠队列与 Stream 消费组", "docs/04-mq.md"),
                item("analytics", "05 实时分析", "PV、HyperLogLog UV、小时桶、热门页", "docs/05-analytics.md"),
                item("lock", "06 分布式锁", "SET NX 安全解锁与 Redisson 看门狗", "docs/06-lock.md"),
                item("pubsub", "07 发布订阅", "消息广播、近期消息和 SSE", "docs/07-pubsub.md"),
                item("ratelimit", "08 限流", "固定窗口、滑动窗口、令牌桶", "docs/08-ratelimit.md"),
                item("expire", "09 数据过期", "TTL、EXPIRE 与过期事件", "docs/09-expire.md"),
                item("page", "10 全页缓存", "渲染结果缓存与更新后删除", "docs/10-page.md"),
                item("social", "11 社交关系", "关注、共同关注、粉丝推送、在线状态", "docs/11-social.md"),
                item("recommend", "12 实时推荐", "兴趣分与物品共现", "docs/12-recommend.md"),
                item("geo", "13 地理位置", "附近的店与两点距离", "docs/13-geo.md"),
                item("timeseries", "14 时间序列", "ZSET 时间索引与按桶聚合", "docs/14-timeseries.md"),
                item("schedule", "15 任务调度", "ZSET 延迟队列，多实例原子领取", "docs/15-schedule.md"),
                item("share", "16 数据共享", "配置哈希与服务心跳", "docs/16-share.md"),
                item("durable", "17 持久化", "查看 RDB / AOF 运行状态", "docs/17-durable.md")
        );
    }

    private static Map<String, String> item(String id, String title, String summary, String doc) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("summary", summary);
        map.put("doc", doc);
        return map;
    }
}

# 01 缓存

代码：`cache/CacheAsideService`、`cache/CacheController`。商品在 MySQL 表 `t_product`。

读多写少的数据先查 Redis，没有再查数据库，再把结果写回 Redis。这就是 Cache-Aside。写的时候先更新数据库，再删除缓存，而不是更新缓存。删除可以避免两个写请求把旧值又写回去。

## 三种常见故障

穿透：请求的 id 数据库里根本没有，缓存也没有，每次都打到数据库。这里做了两层。

- 启动时把已有商品放进 Redisson 布隆过滤器 `cache:product:bloom`。布隆说「没有」就一定没有，直接返回。
- 布隆说「可能有」但数据库确实没有时，写入短 TTL 的占位值 `__NULL__`，避免同一个不存在的 id 被反复查询。

击穿：一个热点键刚好过期，大量请求同时回源。回源前用 `SET key NX EX 5` 抢互斥锁，没抢到的请求等 40 毫秒再读缓存。

雪崩：一大批键在同一秒过期。商品缓存的基础 TTL 是 10 分钟，再加 0 到 120 秒的随机时间。

字典这类很少变的数据用 Spring Cache 的 `@Cacheable`，过期时间在 `CacheConfig` 里统一配成 10 分钟。商品没有用这个注解，因为穿透和击穿需要自己控制回源。

## 键

- `cache:product:{id}` 商品 JSON 或空值占位
- `cache:product:{id}:lock` 重建互斥锁
- `cache:stat:hit`、`cache:stat:miss`、`cache:stat:null` 命中统计

更新商品或扣库存时会删除 `cache:product:{id}`。

## 接口

- `GET /api/cache/products/{id}` 返回 `source`：`HIT`、`MISS`、`BLOOM`、`NULL_CACHE`
- `PUT /api/cache/products/{id}` 更新数据库并删除缓存
- `GET /api/cache/stats`
- `POST /api/cache/bloom/rebuild`
- `GET /api/cache/categories`
- `POST /api/cache/categories/evict`

## 验证

连续两次读取商品 1，第二次 `source` 应为 `HIT`。读取商品 99999，布隆过滤器已建立时应为 `BLOOM`。修改商品后再读，会重新回源。

```bash
curl http://localhost:1236/api/cache/products/1
curl http://localhost:1236/api/cache/products/99999
```

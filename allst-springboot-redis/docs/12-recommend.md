# 12 实时推荐

代码：`recommend/RecommendService`、`recommend/RecommendController`。行为同时写入表 `t_behavior`。

这是一个可以在线更新的物品协同过滤，没有离线任务。

行为权重：

| 行为 | 分 |
| --- | --- |
| view | 1 |
| like | 3 |
| cart | 5 |
| buy | 10 |

每次行为做三件事：

- `ZINCRBY recommend:interest:{user}`，记下用户对商品的兴趣
- `SADD recommend:seen:{user}`，记下已经看过，推荐时排除
- 和该用户最近接触过的商品互相 `ZINCRBY recommend:sim:{item}`，记下「这两个商品经常一起出现」

推荐时取用户兴趣最高的 5 个商品，把它们的相似商品按「兴趣分 × 共现分」加总，去掉已经看过的，再补上商品名。

冷启动用户没有兴趣分，结果是空的。示例数据让 qiu 先看过商品 1，才能根据商品 1 的共现给出其它商品。

这个模型会偏向热门商品，也没有衰减。生产上要加时间衰减、曝光去重和离线校正。MySQL 里的行为流水用来事后核对，在线计算只读 Redis。

## 接口

- `POST /api/recommend/seed`
- `POST /api/recommend/behavior`，体：`{"userId":"ada","itemId":2,"action":"like"}`
- `GET /api/recommend?userId=qiu&size=5`
- `GET /api/recommend/behaviors?userId=ada`

## 验证

点「写入示例行为」，qiu 的推荐里应出现和商品 1 一起被别人看过的商品，且不包含商品 1 本身。

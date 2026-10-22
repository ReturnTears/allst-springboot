# 03 排行榜和计数器

代码：`rank/RankService`、`rank/RankController`。

排行榜用 ZSET `rank:board`。成员是用户，分数用 `ZINCRBY` 累加。前几名用 `ZREVRANGE WITHSCORES`，名次用 `ZREVRANK`。Redis 的名次从 0 开始，接口里加一，变成日常说的第 1 名。

浏览量是字符串计数 `counter:view:{articleId}`，`INCR` 即可。

点赞要去重。用户集合是 `counter:like:users:{articleId}`，总数是 `counter:like:count:{articleId}`。`SADD` 返回 1 才给总数加一；取消时 `SREM` 返回 1 才减一。只做 `INCR` 的话，同一个人连点会把赞数刷上去。

榜单很大时不要一次 `ZRANGE 0 -1`。接口最多返回 50 名。

## 接口

- `POST /api/rank/seed` 写入五名示例选手，会清空原榜
- `POST /api/rank/score`，体：`{"user":"ada","delta":10}`
- `GET /api/rank/top?limit=10`
- `GET /api/rank/me?user=ada`
- `POST /api/rank/articles/{id}/view`
- `POST /api/rank/articles/{id}/like`，体：`{"user":"ada","liked":true}`
- `GET /api/rank/articles/{id}`

## 验证

点「填充榜单」后给 ada 加分，再看前十名，名次应随分数变化。同一用户对同一篇文章点两次赞，`changed` 第二次为 false，赞数不增加。

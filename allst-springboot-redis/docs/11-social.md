# 11 社交功能

代码：`social/SocialService`、`social/SocialController`。

关注关系用两个 Set：

- `social:follow:{user}` 我关注的人
- `social:fans:{user}` 关注我的人

关注和取消要同时改这两个集合。共同关注是两个 follow 集合的 `SINTER`。

发动态时把 JSON 推到作者自己的时间线，再推到每个粉丝的 `social:feed:{user}`，并用 `LTRIM` 只留最近 100 条。这是写扩散：读时间线很快，粉丝特别多时写会很重。示例最多扇出 200 个粉丝。大 V 应该改成读扩散：粉丝打开时间线时再拉取关注的人最近的动态。

在线状态是 `social:online:{user}`，TTL 60 秒。客户端需要在过期前再次上报。人下线后键自己消失，不用专门清理。

## 接口

- `POST /api/social/seed`
- `POST /api/social/follow`，体：`{"user":"ada","target":"lin","follow":true}`
- `GET /api/social/mutual?a=ada&b=lin`
- `POST /api/social/posts`，体：`{"user":"lin","content":"..."}`
- `GET /api/social/feed?user=ada`
- `POST /api/social/online`，体：`{"user":"ada"}`
- `GET /api/social/online?user=ada`
- `GET /api/social/profile?user=ada`

## 验证

点「填充关系」。ada 和 lin 的共同关注里有 max。lin 发的动态会出现在 ada 的时间线里，因为 ada 关注了 lin，写扩散发给了粉丝。ada 的在线 TTL 接近 60 秒。

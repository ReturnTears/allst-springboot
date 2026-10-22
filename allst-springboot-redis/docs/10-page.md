# 10 全页缓存

代码：`pagecache/PageCacheService`、`pagecache/PageCacheController`。页面存在表 `t_page`。

活动页、帮助页读得多、改得少。每次访问都查库再渲染，CPU 和数据库都会被重复工作占住。这里把渲染后的整段 HTML 放进 `page:html:{slug}`，TTL 10 分钟。

命中时直接返回 HTML，响应头 `X-Cache: HIT`。未命中时查库、转义正文、拼页面，再写入缓存，响应头 `X-Cache: MISS`。

正文按纯文本转义，不把数据库内容当成 HTML 执行，避免缓存进一段脚本。

更新正文时只改数据库并删除缓存。下一次访问再渲染。不要在更新请求里直接写入新 HTML：两个更新并发时，后写进缓存的可能是旧正文。

用户相关的页面不能整页共用一个键，否则会把 A 的内容发给 B。这个示例页面没有登录态。

## 接口

- `GET /api/page/{slug}` 返回 HTML
- `GET /api/page/{slug}/view` 返回是否命中、TTL 和 HTML
- `PUT /api/page/{slug}`，体：`{"title":"...","content":"..."}`
- `DELETE /api/page/{slug}/cache`

启动时如果页面表为空，会写入 `home`。

## 验证

打开控制台「10 全页缓存」，第一次「查看是否命中」是 false，第二次是 true。改完标题再加载 iframe，标题应变成新的，并且下一次查看又是未命中。

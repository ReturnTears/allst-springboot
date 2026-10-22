# 3. IP 白名单

只允许名单内的来源访问 `/api/**`，挡住扫描和未登记的调用方。

## 实现

| 类 | 作用 |
| --- | --- |
| `IpWhitelistInterceptor` | 比对白名单，不在名单内抛 `40301` |
| `ClientIpResolver` | 取出客户端 IP |
| `NormProperties.Ip` | 开关、名单、是否信任转发头 |

配置：

```yaml
norm:
  ip:
    enabled: true
    trust-forwarded: false
    whitelist:
      - 127.0.0.1
      - 0:0:0:0:0:0:0:1
      - ::1
```

默认只使用 TCP 连接上的远端地址。`X-Forwarded-For` 和 `X-Real-IP` 可以被调用方自己写，默认不采用。服务部署在可信反向代理后面时，把 `norm.ip.trust-forwarded` 设为 `true`，并保证代理覆盖这两个头。

开启白名单但名单为空时，应用拒绝启动。`::ffff:127.0.0.1` 会先被收成 `127.0.0.1`。

本示例按 IP 精确匹配。网段和地理位置策略放在网关更合适，应用内这份名单是最后一道门。

## 失败

HTTP 403，body.code 为 `40301`，消息里带上被拒绝的 IP，方便把机器地址加进名单。

集成测试用 `8.8.8.8` 访问 `/api/v1/users`，在签名之前就被拒绝。

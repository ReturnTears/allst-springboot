# 1. 签名

防止查询参数和报文体在传输过程中被改掉，并挡住过期请求和原样重放。

## 实现

| 类 | 作用 |
| --- | --- |
| `com.allst.springboot.annotation.RequireSign` | 标在控制器类或方法上才校验 |
| `com.allst.springboot.web.SignatureInterceptor` | 校验时间窗、签名和随机串 |
| `com.allst.springboot.crypto.SignUtils` | 拼接待签名字符串并计算 HMAC-SHA256 |
| `com.allst.springboot.security.NonceStore` | 记住窗口期内用过的随机串 |
| `com.allst.springboot.web.RepeatableReadRequestWrapper` | 报文读一次后仍能交给控制器 |

`/api/health` 和 `/api/dev/**` 没有 `@RequireSign`。开关是 `norm.sign.enabled`。

签名失败不会消耗随机串，调用方改完签名后可以用同一个随机串重试。签名通过之后，同一个随机串在时间窗内再出现会返回 `40103`。异步请求的第二次分发不再校验，避免合法请求被当成重放。

## 待签名字符串

固定 9 行，用 `\n` 连接，缺的字段留空行。方法名转成大写。Query 按参数名排序，使用原始字符串，不解码。

```text
METHOD
PATH
SORTED_QUERY
BODY
APP_ID
TIMESTAMP
NONCE
IDEMPOTENT_KEY
OPERATOR
```

`X-Trace-Id` 不参与签名。幂等键和操作人参与签名，避免有人拿一份合法签名去换幂等键再写一次数据。

`X-Sign` 是上面整段字符串的 HMAC-SHA256，密钥是 `norm.sign.apps` 里该应用的 secret，输出为小写十六进制。

下面这组固定输入的签名是 `2e6719548bfd2983190305d8b2f72afbfb1415082026d3afcbccd385a5461083`，单测 `SignUtilsTest` 锁着这个值：

```text
POST
/api/v1/users

{}
demo-app
1700000000000
abc


```

Python：

```python
import hmac, hashlib
canonical = "POST\n/api/v1/users\n\n{}\ndemo-app\n1700000000000\nabc\n\n"
print(hmac.new(b"demo-secret", canonical.encode(), hashlib.sha256).hexdigest())
```

时间戳是毫秒。与服务器时差超过 `norm.sign.expire-seconds`（默认 300 秒）返回 `40102`。

## 本地怎么调

`POST /api/dev/sign`，body 示例：

```json
{
  "method": "POST",
  "path": "/api/v1/users",
  "body": "{\"username\":\"zhangsan\",\"realName\":\"张三\",\"phone\":\"13800138000\",\"email\":\"zhangsan@example.com\",\"idCard\":\"110101199001011234\"}",
  "idempotentKey": "create-user-0001",
  "operator": "tester"
}
```

返回的 `timestamp`、`nonce`、`sign` 放到请求头里。`body` 必须和真正发出去的 JSON 字节一致，不要让中间工具重新排版。

## 失败码

| code | 含义 |
| --- | --- |
| 40100 | 缺少 `X-App-Id`、`X-Timestamp`、`X-Nonce` 或 `X-Sign` |
| 40101 | 签名不正确 |
| 40102 | 时间戳过期或格式不对 |
| 40103 | 随机串已使用 |
| 40104 | 未知的应用标识 |

## 生产注意

- 密钥放配置中心或环境变量，不要提交真实密钥。演示值是 `demo-app / demo-secret`。
- `norm.dev-tools-enabled` 必须为 `false`，否则任何人都能让服务器替他签名。
- `NonceStore` 只在当前进程有效。多实例要把随机串放到 Redis，并设置与时间窗相同的过期时间。
- 报文编码固定 UTF-8。

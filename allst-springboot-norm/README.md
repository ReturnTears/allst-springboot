# allst-springboot-norm

把接口开发里的 18 条规范做成一条可以跑起来的请求链路。说明按条目写在 [docs](docs/README.md)，方便以后回溯。

默认使用内存数据库 H2，执行下面的命令即可启动，端口 `1234`：

```bash
mvn spring-boot:run
```

| 地址 | 用途 |
| --- | --- |
| http://127.0.0.1:1234/doc.html | Knife4j 接口文档 |
| http://127.0.0.1:1234/swagger-ui.html | Swagger UI |
| http://127.0.0.1:1234/v3/api-docs | OpenAPI JSON |
| http://127.0.0.1:1234/api/health | 健康检查，也是压测目标 |

业务接口在 `/api/v1` 下，需要签名。本地调用方：

- 应用标识 `demo-app`
- 密钥 `demo-secret`
- IP 白名单只放了本机

计算签名可以先调用 `POST /api/dev/sign`。这个接口拿着服务端密钥，**生产环境必须关闭**：`norm.dev-tools-enabled=false`。

## 数据库

| 启动方式 | 说明 |
| --- | --- |
| 默认 | H2 内存库，表由 JPA 自动创建 |
| `--spring.profiles.active=mysql` | 使用 `application-mysql.yml`，先建库 `allst_norm` |
| `--spring.profiles.active=postgres` | 使用 `application-postgres.yml`，先建库 `allst_norm` |

MySQL 建表语句见 `src/main/resources/db/schema-mysql.sql`。实体不绑定某一种数据库，换 PostgreSQL 只改数据源和方言。

启动后会写入两条演示用户 `demo01`、`demo02`。手机号和身份证号在库里是密文，接口返回的是脱敏值。

## 测试和压测

```bash
mvn test
```

压测程序不会随应用启动。先启动服务，再执行：

```bash
mvn -DskipTests compile exec:java -Dexec.mainClass=com.allst.springboot.stress.ApiStressMain -Dexec.args="http://127.0.0.1:1234/api/health 20 200"
```

参数依次是地址、线程数、总请求数。

## 请求经过的位置

```text
客户端
  -> RequestAuditFilter        链路号、可重复读报文、访问日志
  -> HeaderContextInterceptor  解析公共请求头
  -> IpWhitelistInterceptor    IP 白名单
  -> SignatureInterceptor      HMAC 签名、时间窗、随机串
  -> RateLimitAspect           滑动窗口限流
  -> IdempotentAspect          幂等键
  -> Controller                参数校验、请求方法
  -> Service                   分页和批量上限、事务
  -> JPA                       敏感字段加密
  -> Jackson                   返回前脱敏
  -> Result                    统一返回
```

异常在 `GlobalExceptionHandler` 收口，响应体不带堆栈。

## 组装签名
```text
原文固定 9 行，用换行符 \n 连接，没有的字段留空行，最后一行后面不要再多加换行：
METHOD
PATH
按参数名排序后的原始 query
BODY 原始字节,原始请求体也不要有空格
APP_ID
TIMESTAMP
NONCE
IDEMPOTENT_KEY
OPERATOR

例如:
POST
/api/v1/orders

{"userId":1,"amount":19.9,"remark":"货品A:19.9"}
demo-app
1792653597545
a1b2
ikey-0001
admin

按照上述的9行+密码:demo-secret使用HMACSHA256加密算法得出的结果即为签名
HMACSHA256算法在线地址:https://www.jyshare.com/crypto/hmacsha256/
时间戳在线地址:https://tool.lu/timestamp/

```

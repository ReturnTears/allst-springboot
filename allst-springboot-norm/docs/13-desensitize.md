# 13. 数据脱敏

返回给调用方和写进访问日志的内容里，手机号、身份证号、邮箱、姓名按固定规则打码。库里的加密见 [02-encryption.md](02-encryption.md)。两件事解决的问题不同：加密保护存储，脱敏保护展示和日志。

## 实现

在返回对象的字段上标注 `@Desensitize`。`UserVO` 已经标了姓名、手机号、邮箱、身份证号。Jackson 序列化时走 `DesensitizeSerializer`。

不要把这个注解放在入参上，否则校验和入库拿到的会是打码后的值。

| 类型 | 示例 |
| --- | --- |
| `PHONE` | `13800138000` → `138****8000` |
| `ID_CARD` | `110101199001011234` → `110****1234` |
| `EMAIL` | `demo01@example.com` → `d***@example.com` |
| `NAME` | `张三` → `张*`，`张三丰` → `张*丰` |
| `BANK_CARD` | 保留前 4 后 4，规则已实现，当前接口没有银行卡字段 |

日志使用 `SensitiveTextMasker` 的正则，对 JSON 文本里的手机号和身份证号做同样的遮盖，再按长度截断。

规则集中在 `DesensitizeUtils`。改掩码格式时只改这一处，并补 `DesensitizeUtilsTest`。

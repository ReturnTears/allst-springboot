# 2. 加密

手机号和身份证号落库前加密。接口展示用的是脱敏，不是把密文直接返回，也不是把明文返回。

## 实现

| 类 | 作用 |
| --- | --- |
| `AesCipher` | AES-GCM，12 字节随机 IV，128 位认证标签 |
| `AesKeys` | 启动时写入密钥，供 JPA 转换器读取 |
| `AesStringConverter` | 入库加密、读出解密 |
| `UserEntity` | `phone`、`idCard` 使用该转换器 |
| `NormStartupChecker` | 密钥为空时拒绝启动 |

密文格式是 `Base64(IV + 密文 + 认证标签)`。同一明文每次加密结果不同。密钥来自 `norm.crypto.aes-key`，长度必须是 16、24 或 32 字节；其他长度会先做 SHA-256 再取 16 字节。演示密钥是 `1234567890abcdef`。

内存里的实体是明文，业务代码可以继续校验和使用。写响应时由脱敏序列化器处理，见 [13-desensitize.md](13-desensitize.md)。

## 怎么确认已经加密

创建用户后直接查 `norm_user.phone`，列值不是手机号。通过 Repository 读出来的 `phone` 会变回明文。集成测试 `createUserEncryptsAtRestAndMasksInResponse` 覆盖了这两步。

`POST /api/dev/crypto/encrypt` 和 `/api/dev/crypto/decrypt` 用来观察算法输出。生产环境随 `norm.dev-tools-enabled=false` 一起关闭。

## 检索限制

随机 IV 的密文不能按等值查询。要按手机号查找，需要额外的盲索引或确定性加密字段，本示例没有做这一层。

## 生产注意

- 密钥放 KMS 或配置中心，定期轮换时要准备解密旧数据的旧密钥。
- 传输加密靠 HTTPS。这里的 AES 解决的是库文件、备份和日志库泄露。
- 异常信息里不要打印明文。实体没有把手机号和身份证号放进普通业务日志。

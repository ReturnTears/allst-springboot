# 接口演练

服务端口 `8129`。统一返回：

```json
{ "code": 200, "message": "OK", "data": {}, "timestamp": 0 }
```

业务拒绝是 HTTP 400，`message` 为具体原因。乐观锁或唯一约束冲突是 HTTP 409。

下面用低风险订单走完主路径。库存需存在，启动后已有 `SKU-PHONE`。

## 创建

```http
POST /api/trade-orders
Content-Type: application/json

{
  "buyerName": "ada",
  "skuCode": "SKU-PHONE",
  "quantity": 1,
  "amount": 1999.00,
  "riskScore": 10
}
```

`data.status` 为 `CREATED`。`allowedEvents` 里有 `SUBMIT_PAY` 和 `CANCEL`。记下 `orderNo`。

风险分改为 `90` 时，支付成功会进入 `RISK_REVIEW`，而不是直接待占库存。

## 预检

```http
POST /api/trade-orders/precheck

{ "skuCode": "SKU-PHONE", "quantity": 1, "riskScore": 10 }
```

返回风险分、可用库存、是否够用，以及这次并行查询的耗时。它不创建订单，也不占库存。

## 推进

```http
POST /api/trade-orders/{orderNo}/events

{ "event": "SUBMIT_PAY", "operator": "ada", "eventId": "pay-001" }
```

然后再发：

| eventId | event | 额外字段 | 期望状态 |
| --- | --- | --- | --- |
| pay-002 | `PAY_SUCCESS` | 无 | `WAIT_ALLOCATE` |
| alloc-001 | `ALLOCATE` | 无 | `WAIT_SHIP` |
| ship-001 | `SHIP` | `shipmentNo: "SF1001"` | `SHIPPED` |
| sign-001 | `SIGN` | 无 | `DELIVERED` |
| ok-001 | `CONFIRM` | 无 | `COMPLETED` |

重复发送 `eventId = pay-001` 时，`replayed` 为 true，状态不变，流水仍是一条。

不带物流单号发送 `SHIP`，返回“发货需要物流单号”，状态保持待发货。

数量大于库存时，`ALLOCATE` 进入 `STOCK_SHORT`，库存不变。补货后再发一次新的 `eventId`，才会进入待发货并扣减。

## 查询

```http
GET /api/trade-orders/{orderNo}
GET /api/trade-orders/{orderNo}/logs
GET /api/async-tasks
```

主路径的流水应有 6 条。异步回执里应出现 `done:warehouse:{orderNo}` 和 `done:completed:{orderNo}`，各一次。

## 合同

```http
POST /api/contracts
{ "title": "年度采购框架", "amount": 80000 }
```

然后对 `/api/contracts/{contractNo}/events` 依次发送 `SUBMIT`、`LEGAL_PASS`、`FINANCE_PASS`、`SIGN`。`LEGAL_PASS` 之后阶段仍是 `REVIEWING`，法务状态已是通过。`FINANCE_PASS` 之后阶段变为 `WAIT_SIGN`。两边对调顺序也可以汇合。

`LEGAL_REJECT` 之后阶段为 `REJECTED`，再发 `FINANCE_PASS` 会被拒绝。

## 发票

```http
POST /api/invoices
{ "title": "办公用品", "amount": 99.00 }
```

`SUBMIT` 后 `APPROVE`。金额为 `10000` 时，角色不是 `FINANCE` 的 `APPROVE` 会失败，状态保持 `PENDING_REVIEW`；角色改为 `FINANCE` 后可以通过。

```http
GET /api/invoices/meta/plantuml
```

返回这份发票机器的 PlantUML 文本。

已开具的发票用 `RED_FLUSH` 作废。草稿发送 `RED_FLUSH` 会被拒绝。

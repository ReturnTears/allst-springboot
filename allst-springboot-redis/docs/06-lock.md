# 06 分布式锁

代码：`lock/StockLockService`、`lock/LockController`。脚本：`resources/lua/unlock.lua`。

扣库存要保证「读库存、判断、扣减」这段时间只有一个请求在做。锁的粒度是商品：`lock:stock:{productId}`。

## Redisson

`tryLock(3 秒)` 不传租约时间，看门狗会给还在执行的持有者续期。业务做完必须在 `finally` 里 `unlock`，并且先看 `isHeldByCurrentThread`，避免解开别人的锁。

锁里面是一条带条件的 SQL：`stock >= qty` 才扣减。即使锁失效，数据库也不会把库存扣成负数。扣成功后删除商品缓存。

单机 Redis 上这把锁是可靠的。多主、主从切换时，锁可能在切换窗口里丢。Redlock 也不能从原理上消除这个窗口。库存这种不能错的操作，要以数据库条件更新为准，Redis 锁只是减少冲突。

## 手写锁

`SET lock:manual:{name} {token} NX EX {秒}`。token 是随机值。解锁脚本先比较 token，相同才 `DEL`。如果不管是谁都 `DEL`，会把刚加上的另一把锁删掉。

手写锁没有看门狗。过期时间要大于业务耗时，业务也要能容忍锁过期后的并发，所以才保留数据库条件。

## 接口

- `POST /api/lock/stock/deduct`，体：`{"productId":1,"qty":1}`
- `POST /api/lock/manual/try`，体：`{"key":"order","ttlSeconds":15}`
- `POST /api/lock/manual/unlock`，体：`{"key":"order","token":"..."}`

## 验证

对商品 1 连续扣减，库存每次减 1。用错误 token 解锁，`released` 为 false。库存扣到 0 后再扣，返回库存不足。

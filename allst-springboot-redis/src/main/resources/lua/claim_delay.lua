-- 原子领取到期任务：分数小于等于当前时间的成员被移出延迟队列。
-- 多实例同时调度时，ZREM 成功的一方才算领到，避免重复执行。
-- KEYS[1] 延迟队列 ZSET
-- ARGV[1] 当前毫秒  ARGV[2] 单次最多领取条数
local due = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, tonumber(ARGV[2]))
local claimed = {}
for i = 1, #due do
    if redis.call('ZREM', KEYS[1], due[i]) == 1 then
        claimed[#claimed + 1] = due[i]
    end
end
return claimed

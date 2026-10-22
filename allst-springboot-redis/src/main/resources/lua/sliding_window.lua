-- 滑动窗口：用 ZSET 保存每次请求的时间戳，清掉窗口外的成员后再判断数量。
-- KEYS[1] 窗口键
-- ARGV[1] 当前毫秒  ARGV[2] 窗口毫秒  ARGV[3] 限额  ARGV[4] 本次唯一成员
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)
local allowed = 0
if count < limit then
    redis.call('ZADD', key, now, member)
    count = count + 1
    allowed = 1
end
redis.call('PEXPIRE', key, window)
return {allowed, count}

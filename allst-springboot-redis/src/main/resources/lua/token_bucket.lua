-- 令牌桶。桶内令牌按速率补充，单次请求消耗 1 个令牌。全部读写在一次脚本里完成。
-- KEYS[1] 桶状态 Hash
-- ARGV[1] 容量  ARGV[2] 每秒补充速率  ARGV[3] 当前毫秒
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local data = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts = tonumber(data[2])
if tokens == nil then
    tokens = capacity
    ts = now
end
local delta = math.max(0, now - ts) / 1000.0
tokens = math.min(capacity, tokens + delta * rate)
local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end
redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
redis.call('PEXPIRE', key, math.ceil((capacity / math.max(rate, 0.001)) * 2000))
return {allowed, string.format('%.4f', tokens)}

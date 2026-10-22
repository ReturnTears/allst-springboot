-- 固定窗口计数。窗口起点是第一次 INCR 的时刻，只在计数变为 1 时设置过期。
-- KEYS[1] 计数键
-- ARGV[1] 窗口秒数
local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
end
return current

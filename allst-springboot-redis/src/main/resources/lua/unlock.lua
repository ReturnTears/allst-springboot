-- 只有持有者才能删除锁，避免把别人刚拿到的锁解开。
-- KEYS[1] 锁键  ARGV[1] 持有者 token
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0

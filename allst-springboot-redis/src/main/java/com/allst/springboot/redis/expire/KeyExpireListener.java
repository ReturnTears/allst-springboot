package com.allst.springboot.redis.expire;

import com.allst.springboot.redis.common.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 只有 Redis 打开了 notify-keyspace-events 里的 E 和 x（通常写成 Ex）才会收到过期回调。
 * 回调里的键已经不存在了，这里只把键名记到日志列表。不要在回调里做重的业务。
 */
@Slf4j
@Component
public class KeyExpireListener implements MessageListener {

    private final StringRedisTemplate redis;

    public KeyExpireListener(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody(), StandardCharsets.UTF_8);
        if (key.startsWith("expire:log")) {
            return;
        }
        String line = System.currentTimeMillis() + " " + key;
        try {
            redis.opsForList().leftPush(RedisKeys.EXPIRE_LOG, line);
            redis.opsForList().trim(RedisKeys.EXPIRE_LOG, 0, 49);
        } catch (Exception ex) {
            log.debug("记录过期事件失败: {}", ex.getMessage());
        }
    }
}

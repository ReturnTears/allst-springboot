package com.allst.springboot.redis.mq;

import com.allst.springboot.redis.common.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RPOPLPUSH 把消息从主队列原子地挪到处理中列表。
 * 进程在「挪走之后、确认之前」宕机，消息会留在处理中列表里，可以人工或定时重新入队。
 * 这里演示成功路径：写结果并 LREM。主题里包含 fail 的邮件会进死信，用来看失败分支。
 */
@Slf4j
@Component
public class ListQueueConsumer {

    private final StringRedisTemplate redis;
    private final AtomicLong lastWarn = new AtomicLong();

    public ListQueueConsumer(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Scheduled(fixedDelay = 500)
    public void poll() {
        try {
            for (int i = 0; i < 20; i++) {
                String message = redis.opsForList().rightPopAndLeftPush(RedisKeys.MQ_LIST, RedisKeys.MQ_LIST_PROCESSING);
                if (message == null) {
                    return;
                }
                if (message.contains("\"subject\":\"fail") || message.contains("\"subject\":\"FAIL")) {
                    redis.opsForList().remove(RedisKeys.MQ_LIST_PROCESSING, 1, message);
                    push(RedisKeys.MQ_LIST_DEAD, message);
                    continue;
                }
                redis.opsForList().remove(RedisKeys.MQ_LIST_PROCESSING, 1, message);
                push(RedisKeys.MQ_LIST_RESULT, message);
            }
        } catch (Exception ex) {
            warn(ex);
        }
    }

    private void push(String key, String message) {
        redis.opsForList().leftPush(key, message);
        redis.opsForList().trim(key, 0, 49);
    }

    private void warn(Exception ex) {
        long now = System.currentTimeMillis();
        long previous = lastWarn.get();
        if (now - previous > 30_000 && lastWarn.compareAndSet(previous, now)) {
            log.warn("List 队列消费者暂停: {}", ex.getMessage());
        }
    }
}

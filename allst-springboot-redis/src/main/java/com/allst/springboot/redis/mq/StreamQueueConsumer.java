package com.allst.springboot.redis.mq;

import com.allst.springboot.redis.common.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stream 消费组适合要留消费进度的任务。XREADGROUP 的「&gt;」只拿还没投递过的新消息。
 * 示例在处理结束后立刻 XACK。主题包含 fail 时写入死信再确认，避免一条坏消息一直占着 PEL。
 * 生产环境应先重试，再用 XAUTOCLAIM 把超时未确认的消息转给其它消费者。
 */
@Slf4j
@Component
public class StreamQueueConsumer {

    private final StringRedisTemplate redis;
    private final AtomicLong lastWarn = new AtomicLong();

    public StreamQueueConsumer(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @PostConstruct
    public void prepareGroup() {
        try {
            ensureGroup();
        } catch (Exception ex) {
            log.warn("启动时未能创建 Stream 消费组，消费者会在 Redis 可用后重试: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        try {
            ensureGroup();
            List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                    Consumer.from(RedisKeys.MQ_STREAM_GROUP, "worker-1"),
                    StreamReadOptions.empty().count(20),
                    StreamOffset.create(RedisKeys.MQ_STREAM, ReadOffset.lastConsumed()));
            if (records == null) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                Object raw = record.getValue().get("body");
                String body = raw == null ? null : String.valueOf(raw);
                String target = body != null && (body.contains("\"subject\":\"fail") || body.contains("\"subject\":\"FAIL"))
                        ? RedisKeys.MQ_STREAM_DEAD
                        : RedisKeys.MQ_STREAM_RESULT;
                if (body != null) {
                    redis.opsForList().leftPush(target, body);
                    redis.opsForList().trim(target, 0, 49);
                }
                redis.opsForStream().acknowledge(RedisKeys.MQ_STREAM, RedisKeys.MQ_STREAM_GROUP, record.getId());
            }
        } catch (Exception ex) {
            warn(ex);
        }
    }

    private void ensureGroup() {
        try {
            redis.execute((RedisCallback<Object>) connection -> {
                connection.execute("XGROUP",
                        bytes("CREATE"),
                        bytes(RedisKeys.MQ_STREAM),
                        bytes(RedisKeys.MQ_STREAM_GROUP),
                        bytes("0"),
                        bytes("MKSTREAM"));
                return null;
            });
        } catch (Exception ex) {
            if (contains(ex, "BUSYGROUP")) {
                return;
            }
            if (contains(ex, "syntax") || contains(ex, "unknown")) {
                redis.opsForStream().add(RedisKeys.MQ_STREAM, java.util.Collections.singletonMap("bootstrap", "1"));
                redis.opsForStream().createGroup(RedisKeys.MQ_STREAM, ReadOffset.latest(), RedisKeys.MQ_STREAM_GROUP);
                return;
            }
            throw ex;
        }
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private boolean contains(Throwable ex, String text) {
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(text)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void warn(Exception ex) {
        long now = System.currentTimeMillis();
        long previous = lastWarn.get();
        if (now - previous > 30_000 && lastWarn.compareAndSet(previous, now)) {
            log.warn("Stream 消费者暂停: {}", ex.getMessage());
        }
    }
}

package com.allst.springboot.redis.mq;

import com.allst.springboot.redis.common.JsonCodec;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 两种投递方式共用同一封邮件的 JSON。
 * List：LPUSH 进主队列，消费者 RPOPLPUSH 到处理中列表，成功后再删掉。
 * Stream：XADD 进流，消费者按消费组读取并 XACK。
 * 对应文档 docs/04-mq.md。
 */
@Service
public class MqService {

    private final StringRedisTemplate redis;
    private final JsonCodec jsonCodec;

    public MqService(StringRedisTemplate redis, JsonCodec jsonCodec) {
        this.redis = redis;
        this.jsonCodec = jsonCodec;
    }

    public MailMessage enqueueList(String to, String subject, String body) {
        MailMessage message = message(to, subject, body);
        redis.opsForList().leftPush(RedisKeys.MQ_LIST, jsonCodec.toJson(message));
        return message;
    }

    public Map<String, Object> enqueueStream(String to, String subject, String body) {
        MailMessage message = message(to, subject, body);
        RecordId id = redis.opsForStream().add(RedisKeys.MQ_STREAM, Collections.singletonMap("body", jsonCodec.toJson(message)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("streamId", id == null ? null : id.getValue());
        result.put("mail", message);
        return result;
    }

    public Map<String, Object> listSnapshot() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("waiting", size(RedisKeys.MQ_LIST));
        body.put("processing", read(RedisKeys.MQ_LIST_PROCESSING));
        body.put("results", read(RedisKeys.MQ_LIST_RESULT));
        body.put("dead", read(RedisKeys.MQ_LIST_DEAD));
        return body;
    }

    public Map<String, Object> streamSnapshot() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("length", redis.opsForStream().size(RedisKeys.MQ_STREAM));
        body.put("results", read(RedisKeys.MQ_STREAM_RESULT));
        body.put("dead", read(RedisKeys.MQ_STREAM_DEAD));
        return body;
    }

    private MailMessage message(String to, String subject, String body) {
        MailMessage message = new MailMessage();
        message.setId(UUID.randomUUID().toString().replace("-", ""));
        message.setTo(KeyGuard.name(to, "收件人"));
        message.setSubject(trim(subject, "主题"));
        message.setBody(trim(body, "正文"));
        message.setCreatedAt(System.currentTimeMillis());
        return message;
    }

    private String trim(String value, String label) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > 200) {
            throw new com.allst.springboot.redis.common.BizException(label + "长度需在 1 到 200 之间");
        }
        return text;
    }

    private long size(String key) {
        Long size = redis.opsForList().size(key);
        return size == null ? 0L : size;
    }

    private List<String> read(String key) {
        List<String> values = redis.opsForList().range(key, 0, 19);
        return values == null ? Collections.emptyList() : values;
    }
}

package com.allst.springboot.redis.pubsub;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.JsonCodec;
import com.allst.springboot.redis.common.RedisKeys;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 发布端只负责 CONVERT 到频道。真正的扇出发生在订阅回调里：记一份最近消息，并推给当前进程上的 SSE 连接。
 * 发布/订阅不保存离线消息，所以最近 50 条另外写进 List，给刚打开页面的人补看。
 * 对应文档 docs/07-pubsub.md。
 */
@Component
public class NotifyHub implements MessageListener {

    public static final Set<String> CHANNELS = Set.of("notify:broadcast", "notify:order", "notify:im");

    private final StringRedisTemplate redis;
    private final JsonCodec jsonCodec;
    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

    public NotifyHub(StringRedisTemplate redis, JsonCodec jsonCodec) {
        this.redis = redis;
        this.jsonCodec = jsonCodec;
    }

    public Map<String, Object> publish(String channel, String message) {
        String target = normalize(channel);
        if (message == null || message.isBlank() || message.length() > 200) {
            throw new BizException("消息长度需在 1 到 200 之间");
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("channel", target);
        event.put("message", message.trim());
        event.put("ts", System.currentTimeMillis());
        redis.convertAndSend(target, jsonCodec.toJson(event));
        return event;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (Exception ex) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public List<String> recent() {
        List<String> values = redis.opsForList().range(RedisKeys.PUBSUB_RECENT, 0, 19);
        return values == null ? List.of() : values;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        redis.opsForList().leftPush(RedisKeys.PUBSUB_RECENT, body);
        redis.opsForList().trim(RedisKeys.PUBSUB_RECENT, 0, 49);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notify").data(body));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }

    private String normalize(String channel) {
        String name = channel == null ? "" : channel.trim();
        if (!name.startsWith("notify:")) {
            name = "notify:" + name;
        }
        if (!CHANNELS.contains(name)) {
            throw new BizException("频道只允许 broadcast、order、im");
        }
        return name;
    }
}

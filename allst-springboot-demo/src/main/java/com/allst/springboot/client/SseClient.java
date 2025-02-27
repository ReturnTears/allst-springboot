package com.allst.springboot.client;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Hutu
 * @since 2025-02-27 下午 09:30
 */
@Component
public class SseClient {
    private static final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();

    /**
     * 创建连接
     */
    public SseEmitter createSse(String uid) {
        //默认30秒超时,设置为0L则永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        //完成后回调
        sseEmitter.onCompletion(() -> {
            System.out.println(StrUtil.format("[{}]结束连接...................", uid));
            sseEmitterMap.remove(uid);
        });
        //超时回调
        sseEmitter.onTimeout(() -> {
            System.out.println(StrUtil.format("[{}]连接超时...................", uid));
        });
        //异常回调
        sseEmitter.onError(
                throwable -> {
                    try {
                        System.out.println(StrUtil.format("[{}]连接异常,{}", uid, throwable.toString()));
                        sseEmitter.send(SseEmitter.event()
                                .id(uid)
                                .name("发生异常！")
                                .data("发生异常请重试！")
                                .reconnectTime(3000));
                        sseEmitterMap.put(uid, sseEmitter);
                    } catch (IOException e) {
                        System.err.println(StrUtil.format(e.getMessage()));
                    }
                }
        );
        try {
            sseEmitter.send(SseEmitter.event().reconnectTime(5000));
        } catch (IOException e) {
            System.err.println(StrUtil.format(e.getMessage()));
        }
        sseEmitterMap.put(uid, sseEmitter);
        System.out.println(StrUtil.format("[{}]创建sse连接成功！", uid));
        return sseEmitter;
    }

    /**
     * 给指定用户发送消息
     */
    public boolean sendMessage(String uid, String messageId, String message) {
        if (StrUtil.isBlank(message)) {
            System.out.println(StrUtil.format("参数异常，{}为null", uid));
            return false;
        }
        SseEmitter sseEmitter = sseEmitterMap.get(uid);
        if (sseEmitter == null) {
            System.out.println(StrUtil.format("消息推送失败uid:[{}],没有创建连接，请重试。", uid));
            return false;
        }
        try {
            String msg = StrUtil.format("服务器发送消息: {id:{}, body: 'Hello World'}", message);
            sseEmitter.send(SseEmitter.event().id(messageId).reconnectTime(60 * 1000L).data(msg));
            System.out.println(StrUtil.format("用户{},消息id:{},推送成功:{}", uid, messageId, message));
            return true;
        } catch (Exception e) {
            sseEmitterMap.remove(uid);
            System.out.println(StrUtil.format("用户{},消息id:{},推送异常:{}", uid, messageId, e.getMessage()));
            sseEmitter.complete();
            return false;
        }
    }

    /**
     * 断开
     */
    public void closeSse(String uid) {
        if (sseEmitterMap.containsKey(uid)) {
            SseEmitter sseEmitter = sseEmitterMap.get(uid);
            sseEmitter.complete();
            sseEmitterMap.remove(uid);
        } else {
            System.out.println(StrUtil.format("用户{} 连接已关闭", uid));
        }
    }
}

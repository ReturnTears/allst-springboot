package com.allst.springboot.support;

/**
 * 缓存装饰器与幂等命令共同依赖的小接口。
 * 目的不是抽象「缓存」，而是让示例在没有 Redis 的环境里也能跑，
 * 同时在需要时一行配置切到真实 Redis。
 */
public interface KeyValueStore {

    String get(String key);

    void put(String key, String value, long ttlSeconds);

    /** 返回 true 表示本次写入成功，即此前不存在（幂等判断依赖这个语义） */
    boolean putIfAbsent(String key, String value, long ttlSeconds);

    void delete(String key);
}

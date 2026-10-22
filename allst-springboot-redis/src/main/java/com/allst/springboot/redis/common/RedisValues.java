package com.allst.springboot.redis.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RedisValues {

    private RedisValues() {
    }

    public static Map<String, String> asMap(Map<Object, Object> raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null) {
            return map;
        }
        raw.forEach((key, value) -> map.put(String.valueOf(key), value == null ? null : String.valueOf(value)));
        return map;
    }
}

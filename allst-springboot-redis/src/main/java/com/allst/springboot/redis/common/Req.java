package com.allst.springboot.redis.common;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 从 JSON 对象里取参数。示例接口字段少，用 Map 接收，避免为每个接口再堆一个空 DTO。
 */
public final class Req {

    private Req() {
    }

    public static String str(Map<String, Object> body, String key) {
        Object value = required(body, key);
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equals(text)) {
            throw new BizException("缺少参数 " + key);
        }
        return text;
    }

    public static String optional(Map<String, Object> body, String key, String defaultValue) {
        if (body == null || body.get(key) == null) {
            return defaultValue;
        }
        String text = String.valueOf(body.get(key)).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    public static int integer(Map<String, Object> body, String key) {
        return number(required(body, key), key).intValue();
    }

    public static int integer(Map<String, Object> body, String key, int defaultValue) {
        if (body == null || body.get(key) == null || String.valueOf(body.get(key)).isBlank()) {
            return defaultValue;
        }
        return number(body.get(key), key).intValue();
    }

    public static long longValue(Map<String, Object> body, String key) {
        return number(required(body, key), key).longValue();
    }

    public static long longValue(Map<String, Object> body, String key, long defaultValue) {
        if (body == null || body.get(key) == null || String.valueOf(body.get(key)).isBlank()) {
            return defaultValue;
        }
        return number(body.get(key), key).longValue();
    }

    public static double doubleValue(Map<String, Object> body, String key) {
        return number(required(body, key), key).doubleValue();
    }

    public static BigDecimal decimal(Map<String, Object> body, String key) {
        try {
            return new BigDecimal(str(body, key));
        } catch (NumberFormatException ex) {
            throw new BizException("参数 " + key + " 不是合法数字");
        }
    }

    private static Object required(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            throw new BizException("缺少参数 " + key);
        }
        return body.get(key);
    }

    private static Number number(Object value, String key) {
        if (value instanceof Number) {
            return (Number) value;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new BizException("参数 " + key + " 不是合法数字");
        }
    }
}

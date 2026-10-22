package com.allst.springboot.crypto;

/**
 * JPA 转换器不走 Spring 注入，密钥由启动检查器写入这里。
 */
public final class AesKeys {

    private static volatile String key;

    private AesKeys() {
    }

    public static void set(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("AES 密钥不能为空");
        }
        key = value;
    }

    public static String get() {
        if (key == null) {
            throw new IllegalStateException("AES 密钥尚未初始化");
        }
        return key;
    }
}

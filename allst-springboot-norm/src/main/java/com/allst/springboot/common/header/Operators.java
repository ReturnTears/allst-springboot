package com.allst.springboot.common.header;

/**
 * 操作人来自请求头，限制长度后再落库。
 */
public final class Operators {

    private Operators() {
    }

    public static String normalize(String operator) {
        if (operator == null || operator.isBlank()) {
            return "anonymous";
        }
        String value = operator.trim();
        return value.length() <= 64 ? value : value.substring(0, 64);
    }
}

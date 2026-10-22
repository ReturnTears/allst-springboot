package com.allst.springboot.crypto;

/**
 * 脱敏规则集中在一处，序列化和日志共用，避免各写各的掩码。
 */
public final class DesensitizeUtils {

    private DesensitizeUtils() {
    }

    public static String mask(String value, DesensitizeType type) {
        if (value == null || value.isEmpty() || type == null) {
            return value;
        }
        switch (type) {
            case PHONE:
            case ID_CARD:
                return maskMiddle(value, 3, 4);
            case BANK_CARD:
                return maskMiddle(value, 4, 4);
            case EMAIL:
                return maskEmail(value);
            case NAME:
                return maskName(value);
            default:
                return value;
        }
    }

    private static String maskMiddle(String value, int prefix, int suffix) {
        if (value.length() <= prefix + suffix) {
            return stars(value.length());
        }
        return value.substring(0, prefix) + "****" + value.substring(value.length() - suffix);
    }

    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return stars(value.length());
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private static String maskName(String value) {
        if (value.length() == 1) {
            return "*";
        }
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*" + value.charAt(value.length() - 1);
    }

    private static String stars(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append('*');
        }
        return builder.toString();
    }
}

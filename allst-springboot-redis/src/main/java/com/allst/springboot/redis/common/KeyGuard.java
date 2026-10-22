package com.allst.springboot.redis.common;

/**
 * 用户输入会进入 Redis 键或成员时先收口，避免空白、控制字符和过长字符串把键空间弄脏。
 */
public final class KeyGuard {

    private KeyGuard() {
    }

    public static String name(String raw, String label) {
        if (raw == null) {
            throw new BizException(label + "不能为空");
        }
        String text = raw.trim();
        if (text.isEmpty() || text.length() > 160) {
            throw new BizException(label + "长度需在 1 到 160 之间");
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isISOControl(ch) || ch == ' ' || ch == '*' || ch == '?') {
                throw new BizException(label + "不能包含空白、控制字符或通配符");
            }
        }
        return text;
    }

    public static String token(String raw) {
        if (raw == null || !raw.matches("[a-fA-F0-9]{32}")) {
            throw new BizException("登录凭证无效或已过期");
        }
        return raw.toLowerCase();
    }

    public static String slug(String raw) {
        if (raw == null || !raw.matches("[a-z0-9-]{1,64}")) {
            throw new BizException("页面标识只允许小写字母、数字和中划线");
        }
        return raw;
    }
}

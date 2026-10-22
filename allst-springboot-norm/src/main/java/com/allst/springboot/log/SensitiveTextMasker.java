package com.allst.springboot.log;

import java.util.regex.Pattern;

/**
 * 访问日志里的手机号和身份证号打码，避免日志成为另一份明文库。
 */
public final class SensitiveTextMasker {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{6})\\d{8}(\\d{3}[\\dXx])(?!\\d)");

    private SensitiveTextMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = PHONE.matcher(text).replaceAll("$1****$2");
        return ID_CARD.matcher(masked).replaceAll("$1********$2");
    }

    public static String truncate(String text, int max) {
        if (text == null || max < 0 || text.length() <= max) {
            return text;
        }
        String suffix = "...(truncated)";
        if (max <= suffix.length()) {
            return text.substring(0, max);
        }
        return text.substring(0, max - suffix.length()) + suffix;
    }
}

package com.allst.springboot.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * HMAC-SHA256 签名。待签名字符串固定 9 行，缺失字段用空行占位，避免调用方漏字段时还能对上。
 * 对应规范：1 签名。
 */
public final class SignUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private SignUtils() {
    }

    public static String canonical(String method, String path, String query, String body,
                                   String appId, String timestamp, String nonce,
                                   String idempotentKey, String operator) {
        return String.join("\n", Arrays.asList(
                method == null ? "" : method.toUpperCase(Locale.ROOT),
                path == null ? "" : path,
                normalizeQuery(query),
                body == null ? "" : body,
                appId == null ? "" : appId,
                timestamp == null ? "" : timestamp,
                nonce == null ? "" : nonce,
                idempotentKey == null ? "" : idempotentKey,
                operator == null ? "" : operator
        ));
    }

    /**
     * 按参数名排序原始 query，不解码。调用方签名的内容必须和线上字节一致。
     */
    public static String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String part : query.split("&")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        parts.sort(Comparator.comparing((String part) -> {
            int index = part.indexOf('=');
            return index < 0 ? part : part.substring(0, index);
        }).thenComparing(part -> part));
        return String.join("&", parts);
    }

    public static String sign(String canonical, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            char[] out = new char[raw.length * 2];
            for (int i = 0; i < raw.length; i++) {
                int value = raw[i] & 0xff;
                out[i * 2] = HEX[value >>> 4];
                out[i * 2 + 1] = HEX[value & 0x0f];
            }
            return new String(out);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("计算签名失败", ex);
        }
    }

    public static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}

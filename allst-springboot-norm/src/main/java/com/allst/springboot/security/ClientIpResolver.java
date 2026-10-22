package com.allst.springboot.security;

import javax.servlet.http.HttpServletRequest;

/**
 * 解析客户端 IP。默认只认 TCP 远端地址。
 * 对应规范：3 IP 白名单。
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request, boolean trustForwarded) {
        String ip = request.getRemoteAddr();
        if (trustForwarded) {
            String forwarded = first(request.getHeader("X-Forwarded-For"));
            if (forwarded != null) {
                ip = forwarded;
            } else {
                String realIp = blankToNull(request.getHeader("X-Real-IP"));
                if (realIp != null) {
                    ip = realIp;
                }
            }
        }
        return normalize(ip);
    }

    public static String normalize(String ip) {
        if (ip == null) {
            return "";
        }
        String value = ip.trim();
        if (value.startsWith("::ffff:")) {
            value = value.substring("::ffff:".length());
        }
        return value;
    }

    private static String first(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.split(",")[0].trim();
        return value.isEmpty() ? null : value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

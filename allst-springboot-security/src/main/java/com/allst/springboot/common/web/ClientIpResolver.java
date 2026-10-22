package com.allst.springboot.common.web;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 登录限流使用的客户端地址。默认不信任转发头，避免调用方伪造地址绕过限流。
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request, boolean trustForwarded) {
        if (trustForwarded) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                int comma = forwarded.indexOf(',');
                String first = comma < 0 ? forwarded : forwarded.substring(0, comma);
                return first.trim();
            }
        }
        return request.getRemoteAddr();
    }
}

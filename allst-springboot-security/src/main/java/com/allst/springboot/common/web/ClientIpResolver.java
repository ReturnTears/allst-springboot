package com.allst.springboot.common.web;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 登录限流用的客户端地址。默认只用 TCP 连接上的地址。
 * {@code X-Forwarded-For} 是调用方自己填的，信任它的话换一个头就能换一个计数桶，限流会被绕开。
 * 只有请求确实经过你控制的反向代理时，才把 {@code security.login.trust-forwarded} 设为 true。
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

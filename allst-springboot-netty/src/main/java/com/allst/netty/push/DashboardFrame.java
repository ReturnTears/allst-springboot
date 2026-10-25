package com.allst.netty.push;

/**
 * 推给看板和 SSE 的一条实时消息。payload 按 type 变化，见 docs/07-WebSocket看板.md。
 */
public record DashboardFrame(
        String type,
        String deviceId,
        String requestId,
        long timestamp,
        Object payload
) {

    public static DashboardFrame of(String type, String deviceId, String requestId, Object payload) {
        return new DashboardFrame(type, deviceId, requestId, System.currentTimeMillis(), payload);
    }
}

package com.allst.netty.session;

/**
 * 对外暴露的设备快照。不包含 Channel。
 */
public record DeviceView(
        String deviceId,
        String remoteAddress,
        long connectedAt,
        long lastSeenAt,
        boolean active
) {

    public static DeviceView from(DeviceSession session) {
        return new DeviceView(
                session.deviceId(),
                session.remoteAddress(),
                session.connectedAt(),
                session.lastSeenAt(),
                session.active()
        );
    }
}

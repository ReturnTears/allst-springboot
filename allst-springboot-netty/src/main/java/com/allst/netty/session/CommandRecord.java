package com.allst.netty.session;

import java.util.Map;

/**
 * 一次下发给设备的指令。HTTP 接口立刻返回 SENT，设备 ACK 后再变成 ACKED。
 */
public record CommandRecord(
        String requestId,
        String deviceId,
        String action,
        Map<String, Object> params,
        CommandStatus status,
        long createdAt,
        Long ackedAt
) {

    public CommandRecord {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public CommandRecord acked(long ackedAt) {
        return new CommandRecord(requestId, deviceId, action, params, CommandStatus.ACKED, createdAt, ackedAt);
    }

    public CommandRecord failed() {
        return new CommandRecord(requestId, deviceId, action, params, CommandStatus.FAILED, createdAt, ackedAt);
    }
}

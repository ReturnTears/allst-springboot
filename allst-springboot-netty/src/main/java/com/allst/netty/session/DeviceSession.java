package com.allst.netty.session;

import io.netty.channel.Channel;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 一台已登录设备的内存会话。Channel 只在接入层内部使用，不会序列化给 HTTP。
 */
public final class DeviceSession {

    private final String deviceId;
    private final Channel channel;
    private final String remoteAddress;
    private final long connectedAt;
    private final AtomicLong lastSeenAt;

    public DeviceSession(String deviceId, Channel channel, String remoteAddress, long connectedAt) {
        this.deviceId = deviceId;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
        this.connectedAt = connectedAt;
        this.lastSeenAt = new AtomicLong(connectedAt);
    }

    public String deviceId() {
        return deviceId;
    }

    public Channel channel() {
        return channel;
    }

    public String remoteAddress() {
        return remoteAddress;
    }

    public long connectedAt() {
        return connectedAt;
    }

    public long lastSeenAt() {
        return lastSeenAt.get();
    }

    public boolean active() {
        return channel.isActive();
    }

    public void touch() {
        lastSeenAt.set(System.currentTimeMillis());
    }
}

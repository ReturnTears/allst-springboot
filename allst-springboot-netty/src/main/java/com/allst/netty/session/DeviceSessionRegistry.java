package com.allst.netty.session;

import io.netty.channel.Channel;

import java.net.SocketAddress;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 设备号到连接的登记表。同一设备重连时关掉旧连接，避免一台枪占着两条会话。
 */
public final class DeviceSessionRegistry {

    private final ConcurrentMap<String, DeviceSession> byDevice = new ConcurrentHashMap<>();

    public DeviceSession bind(String deviceId, Channel channel) {
        long now = System.currentTimeMillis();
        DeviceSession session = new DeviceSession(deviceId, channel, remote(channel), now);
        channel.attr(DeviceAttrs.DEVICE_ID).set(deviceId);
        DeviceSession previous = byDevice.put(deviceId, session);
        if (previous != null && previous.channel() != channel) {
            previous.channel().close();
        }
        return session;
    }

    /**
     * @return 这次关闭的连接仍然是登记表里的当前会话时返回 true
     */
    public boolean unbindChannel(Channel channel) {
        String deviceId = channel.attr(DeviceAttrs.DEVICE_ID).get();
        if (deviceId == null) {
            return false;
        }
        DeviceSession current = byDevice.get(deviceId);
        if (current == null || current.channel() != channel) {
            return false;
        }
        return byDevice.remove(deviceId, current);
    }

    public Optional<DeviceSession> find(String deviceId) {
        return Optional.ofNullable(byDevice.get(deviceId));
    }

    public List<DeviceView> list() {
        return byDevice.values().stream()
                .map(DeviceView::from)
                .sorted(Comparator.comparing(DeviceView::deviceId))
                .toList();
    }

    private static String remote(Channel channel) {
        SocketAddress address = channel.remoteAddress();
        return address == null ? "unknown" : address.toString();
    }
}

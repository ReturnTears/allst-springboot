package com.allst.netty.session;

public final class DeviceOfflineException extends RuntimeException {

    private final String deviceId;

    public DeviceOfflineException(String deviceId) {
        super("device offline: " + deviceId);
        this.deviceId = deviceId;
    }

    public String deviceId() {
        return deviceId;
    }
}

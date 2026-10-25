package com.allst.netty.session;

public final class DeviceBusyException extends RuntimeException {

    private final String deviceId;

    public DeviceBusyException(String deviceId) {
        super("device write buffer is full: " + deviceId);
        this.deviceId = deviceId;
    }

    public String deviceId() {
        return deviceId;
    }
}

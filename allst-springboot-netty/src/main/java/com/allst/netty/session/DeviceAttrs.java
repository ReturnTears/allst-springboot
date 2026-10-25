package com.allst.netty.session;

import io.netty.util.AttributeKey;

/**
 * 连接上绑定的设备号。登录成功后写入，断线时用它找回会话。
 */
public final class DeviceAttrs {

    public static final AttributeKey<String> DEVICE_ID = AttributeKey.valueOf("warehouseDeviceId");

    private DeviceAttrs() {
    }
}

package com.allst.netty.protocol;

/**
 * 设备通道上的业务消息类型。指令只允许服务端下发，设备侧只回 ACK。
 */
public enum MessageType {
    LOGIN,
    HEARTBEAT,
    ACK,
    SCAN,
    COMMAND,
    LOGOUT,
    ERROR
}

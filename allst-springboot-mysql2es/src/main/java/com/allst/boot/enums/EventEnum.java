package com.allst.boot.enums;

import lombok.Getter;

/**
 * @author Hutu
 * @since 2024-08-09 下午 09:46
 */
@Getter
public enum EventEnum {
    EMAIL("email", "emailMessageEventHandler"),
    SMS("sms", "smsMessageEventHandler");

    private final String msg;
    private final String handler;

    EventEnum(String msg, String handler) {
        this.msg = msg;
        this.handler = handler;
    }

    public static String getHandler(String msg) {
        for (EventEnum eventEnum : EventEnum.values()) {
            if (eventEnum.getMsg().equals(msg)) {
                return eventEnum.getHandler();
            }
        }
        return null;
    }
}

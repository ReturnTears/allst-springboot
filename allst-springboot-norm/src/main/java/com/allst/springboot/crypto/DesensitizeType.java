package com.allst.springboot.crypto;

/**
 * 展示层脱敏类型。加密解决落库泄露，脱敏解决接口和日志把明文带出。
 */
public enum DesensitizeType {
    PHONE,
    ID_CARD,
    EMAIL,
    NAME,
    BANK_CARD
}

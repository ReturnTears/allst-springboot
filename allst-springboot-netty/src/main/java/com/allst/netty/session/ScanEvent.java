package com.allst.netty.session;

import com.allst.netty.protocol.WarehouseMessage;

import java.util.Map;

/**
 * 一次扫码。从设备报文的 payload 里抽出仓内关心的三个字段。
 */
public record ScanEvent(
        String deviceId,
        String requestId,
        long timestamp,
        String barcode,
        String location,
        int quantity
) {

    public static ScanEvent from(WarehouseMessage message) {
        Map<String, Object> payload = message.payload();
        return new ScanEvent(
                message.deviceId(),
                message.requestId(),
                message.timestamp(),
                stringValue(payload.get("barcode")),
                stringValue(payload.get("location")),
                numberValue(payload.get("quantity"))
        );
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ex) {
                return 1;
            }
        }
        return 1;
    }
}

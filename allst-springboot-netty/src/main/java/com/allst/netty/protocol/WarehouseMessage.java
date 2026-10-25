package com.allst.netty.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 帧体 JSON。字段保持扁平，方便扫码枪固件和看板共用同一份结构。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WarehouseMessage(
        MessageType type,
        String requestId,
        String deviceId,
        long timestamp,
        Map<String, Object> payload
) {

    public WarehouseMessage {
        if (payload == null || payload.isEmpty()) {
            payload = Map.of();
        } else {
            payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        }
    }

    public static WarehouseMessage login(String deviceId) {
        return create(MessageType.LOGIN, deviceId, null, Map.of());
    }

    public static WarehouseMessage heartbeat(String deviceId) {
        return create(MessageType.HEARTBEAT, deviceId, null, Map.of());
    }

    public static WarehouseMessage ack(String deviceId, String requestId) {
        return create(MessageType.ACK, deviceId, requestId, Map.of());
    }

    public static WarehouseMessage scan(String deviceId, String barcode, String location, int quantity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("barcode", barcode);
        payload.put("location", location);
        payload.put("quantity", quantity);
        return create(MessageType.SCAN, deviceId, null, payload);
    }

    public static WarehouseMessage command(String deviceId, String action, Map<String, Object> params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        if (params != null && !params.isEmpty()) {
            payload.put("params", params);
        }
        return create(MessageType.COMMAND, deviceId, null, payload);
    }

    public static WarehouseMessage error(String deviceId, String requestId, String reason) {
        return create(MessageType.ERROR, deviceId == null ? "" : deviceId, requestId, Map.of("reason", reason));
    }

    private static WarehouseMessage create(MessageType type, String deviceId, String requestId, Map<String, Object> payload) {
        String id = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        return new WarehouseMessage(type, id, deviceId, System.currentTimeMillis(), payload);
    }
}

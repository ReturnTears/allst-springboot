package com.allst.springboot.warehouse.api;

public record GatewayMeta(
        int httpPort,
        int tcpPort,
        int websocketPort,
        String websocketPath,
        boolean simulatorEnabled
) {
}

package com.allst.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 仓储扫码设备接入网关。
 * HTTP 由 WebFlux 的 Reactor Netty 提供，设备长连接和看板 WebSocket 由另外两组 Netty 提供。
 * 阅读顺序从 README.md 进入 docs。
 */
@SpringBootApplication
public class AllstSpringbootNettyApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllstSpringbootNettyApplication.class, args);
    }
}

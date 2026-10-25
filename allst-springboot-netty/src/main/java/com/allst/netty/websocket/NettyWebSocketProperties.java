package com.allst.netty.websocket;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 仓管看板的独立 WebSocket 端口。它和 WebFlux 的 8080 不是同一个 Netty。
 */
@Validated
@ConfigurationProperties(prefix = "netty.websocket")
public class NettyWebSocketProperties {

    private boolean enabled = true;

    @NotBlank
    private String host = "0.0.0.0";

    @Min(0)
    @Max(65535)
    private int port = 9001;

    @NotBlank
    private String path = "/ws/dashboard";

    @Min(1)
    private int bossThreads = 1;

    /** 0 表示使用 Netty 默认值。 */
    @Min(0)
    private int workerThreads = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }
}

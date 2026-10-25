package com.allst.netty.tcp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 设备 TCP 接入端口。worker-threads 为 0 时交给 Netty 按 CPU 核数决定。
 */
@Validated
@ConfigurationProperties(prefix = "netty.server")
public class NettyServerProperties {

    private boolean enabled = true;

    @NotBlank
    private String host = "0.0.0.0";

    @Min(0)
    @Max(65535)
    private int port = 9000;

    @Min(1)
    private int bossThreads = 1;

    /** 0 表示使用 Netty 默认值，即最多 CPU 核数的两倍。 */
    @Min(0)
    private int workerThreads = 0;

    /** 0 表示按 CPU 核数创建业务线程，至少 2 条。 */
    @Min(0)
    private int businessThreads = 0;

    @Min(1)
    private int readerIdleSeconds = 90;

    /** 含 7 字节帧头。扫码报文很小，大文件不要走这条长连接。 */
    @Min(8)
    private int maxFrameLength = 65_536;

    @Min(1)
    private int soBacklog = 1024;

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

    public int getBusinessThreads() {
        return businessThreads;
    }

    public void setBusinessThreads(int businessThreads) {
        this.businessThreads = businessThreads;
    }

    public int getReaderIdleSeconds() {
        return readerIdleSeconds;
    }

    public void setReaderIdleSeconds(int readerIdleSeconds) {
        this.readerIdleSeconds = readerIdleSeconds;
    }

    public int getMaxFrameLength() {
        return maxFrameLength;
    }

    public void setMaxFrameLength(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    public int getSoBacklog() {
        return soBacklog;
    }

    public void setSoBacklog(int soBacklog) {
        this.soBacklog = soBacklog;
    }
}

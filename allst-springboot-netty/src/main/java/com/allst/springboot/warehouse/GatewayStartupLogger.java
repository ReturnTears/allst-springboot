package com.allst.springboot.warehouse;

import com.allst.netty.client.DeviceSimulator;
import com.allst.netty.client.NettyClientProperties;
import com.allst.netty.tcp.NettyServerProperties;
import com.allst.netty.tcp.NettyTcpServerLifecycle;
import com.allst.netty.websocket.NettyWebSocketProperties;
import com.allst.netty.websocket.NettyWebSocketServerLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动完成后把三个端口打到日志里，方便对照线程名和文档。
 */
@Component
public class GatewayStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GatewayStartupLogger.class);

    private final Environment environment;
    private final NettyServerProperties serverProperties;
    private final NettyClientProperties clientProperties;
    private final NettyWebSocketProperties webSocketProperties;
    private final ObjectProvider<NettyTcpServerLifecycle> tcpServer;
    private final ObjectProvider<NettyWebSocketServerLifecycle> webSocketServer;
    private final ObjectProvider<DeviceSimulator> simulator;

    public GatewayStartupLogger(Environment environment,
                                NettyServerProperties serverProperties,
                                NettyClientProperties clientProperties,
                                NettyWebSocketProperties webSocketProperties,
                                ObjectProvider<NettyTcpServerLifecycle> tcpServer,
                                ObjectProvider<NettyWebSocketServerLifecycle> webSocketServer,
                                ObjectProvider<DeviceSimulator> simulator) {
        this.environment = environment;
        this.serverProperties = serverProperties;
        this.clientProperties = clientProperties;
        this.webSocketProperties = webSocketProperties;
        this.tcpServer = tcpServer;
        this.webSocketServer = webSocketServer;
        this.simulator = simulator;
    }

    @Override
    public void run(ApplicationArguments args) {
        int httpPort = environment.getProperty("local.server.port", Integer.class, 8080);
        NettyTcpServerLifecycle tcp = tcpServer.getIfAvailable();
        NettyWebSocketServerLifecycle webSocket = webSocketServer.getIfAvailable();
        log.info("仓管看板    http://localhost:{}", httpPort);
        if (tcp == null) {
            log.info("设备 TCP    未开启");
        } else {
            log.info("设备 TCP    {}:{}，模拟器拨号 {}:{}", serverProperties.getHost(), tcp.localPort(), clientProperties.getHost(), clientProperties.getPort());
        }
        if (webSocket == null) {
            log.info("看板 WS     未开启");
        } else {
            log.info("看板 WS     ws://localhost:{}{}", webSocket.localPort(), webSocketProperties.getPath());
        }
        if (simulator.getIfAvailable() == null) {
            log.info("模拟扫码枪  未开启");
            return;
        }
        StringBuilder devices = new StringBuilder();
        for (int index = 1; index <= clientProperties.getDeviceCount(); index++) {
            if (index > 1) {
                devices.append(", ");
            }
            devices.append(DeviceSimulator.deviceId(index));
        }
        log.info("模拟扫码枪  {}", devices);
    }
}

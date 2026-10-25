package com.allst.netty.autoconfigure;

import com.allst.netty.client.DeviceSimulator;
import com.allst.netty.client.NettyClientProperties;
import com.allst.netty.protocol.WarehouseFrameEncoder;
import com.allst.netty.protocol.WarehouseMessageDecoder;
import com.allst.netty.push.RealtimePushService;
import com.allst.netty.session.CommandTracker;
import com.allst.netty.session.DeviceSessionRegistry;
import com.allst.netty.session.RecentEventLog;
import com.allst.netty.tcp.NettyServerProperties;
import com.allst.netty.tcp.NettyTcpServerLifecycle;
import com.allst.netty.tcp.WarehouseServerHandler;
import com.allst.netty.websocket.DashboardWebSocketHandler;
import com.allst.netty.websocket.NettyWebSocketProperties;
import com.allst.netty.websocket.NettyWebSocketServerLifecycle;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 接入网关的自动配置。
 * 这个类不在启动类的组件扫描范围内，只通过
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 装入。
 * 说明见 docs/10-自动配置.md。
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnClass(ServerBootstrap.class)
@EnableConfigurationProperties({
        NettyServerProperties.class,
        NettyClientProperties.class,
        NettyWebSocketProperties.class
})
public class NettyGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WarehouseFrameEncoder warehouseFrameEncoder(ObjectMapper objectMapper) {
        return new WarehouseFrameEncoder(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public WarehouseMessageDecoder warehouseMessageDecoder(ObjectMapper objectMapper) {
        return new WarehouseMessageDecoder(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DeviceSessionRegistry deviceSessionRegistry() {
        return new DeviceSessionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public RecentEventLog recentEventLog() {
        return new RecentEventLog(500);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandTracker commandTracker() {
        return new CommandTracker();
    }

    @Bean
    @ConditionalOnMissingBean
    public RealtimePushService realtimePushService(DeviceSessionRegistry sessions,
                                                    RecentEventLog events,
                                                    ObjectMapper objectMapper) {
        return new RealtimePushService(sessions, events, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "netty.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WarehouseServerHandler warehouseServerHandler(DeviceSessionRegistry sessions,
                                                         RecentEventLog events,
                                                         CommandTracker commands,
                                                         RealtimePushService push) {
        return new WarehouseServerHandler(sessions, events, commands, push);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "netty.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public NettyTcpServerLifecycle nettyTcpServerLifecycle(NettyServerProperties properties,
                                                           WarehouseFrameEncoder encoder,
                                                           WarehouseMessageDecoder decoder,
                                                           WarehouseServerHandler handler) {
        return new NettyTcpServerLifecycle(properties, encoder, decoder, handler);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "netty.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DashboardWebSocketHandler dashboardWebSocketHandler(RealtimePushService push, ObjectMapper objectMapper) {
        return new DashboardWebSocketHandler(push, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "netty.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
    public NettyWebSocketServerLifecycle nettyWebSocketServerLifecycle(NettyWebSocketProperties properties,
                                                                       DashboardWebSocketHandler handler) {
        return new NettyWebSocketServerLifecycle(properties, handler);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "netty.client",
            name = {"enabled", "simulator-enabled"},
            havingValue = "true",
            matchIfMissing = true
    )
    public DeviceSimulator deviceSimulator(NettyClientProperties properties,
                                           WarehouseFrameEncoder encoder,
                                           WarehouseMessageDecoder decoder) {
        return new DeviceSimulator(properties, encoder, decoder);
    }
}

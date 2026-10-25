package com.allst.netty.websocket;

import com.allst.netty.tcp.NettyThreads;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.net.InetSocketAddress;

/**
 * 看板 WebSocket 服务。和设备 TCP、WebFlux HTTP 各用一组线程。
 */
public final class NettyWebSocketServerLifecycle implements SmartLifecycle {

    public static final int PHASE = 0;

    private static final Logger log = LoggerFactory.getLogger(NettyWebSocketServerLifecycle.class);

    private final NettyWebSocketProperties properties;
    private final DashboardWebSocketHandler handler;

    private volatile boolean running;
    private EventExecutorGroup boss;
    private EventExecutorGroup worker;
    private Channel channel;

    public NettyWebSocketServerLifecycle(NettyWebSocketProperties properties, DashboardWebSocketHandler handler) {
        this.properties = properties;
        this.handler = handler;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(properties.getBossThreads(), new DefaultThreadFactory("dashboard-boss"));
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(properties.getWorkerThreads(), new DefaultThreadFactory("dashboard-worker"));
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new DashboardWebSocketInitializer(properties.getPath(), handler));
            Channel bound = bootstrap.bind(properties.getHost(), properties.getPort()).syncUninterruptibly().channel();
            this.boss = bossGroup;
            this.worker = workerGroup;
            this.channel = bound;
            this.running = true;
            log.info("仓管看板 WebSocket 已监听 {} 路径 {}", bound.localAddress(), properties.getPath());
        } catch (RuntimeException ex) {
            NettyThreads.shutdown(bossGroup);
            NettyThreads.shutdown(workerGroup);
            throw ex;
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        Channel current = this.channel;
        if (current != null) {
            current.close().syncUninterruptibly();
        }
        NettyThreads.shutdown(boss);
        NettyThreads.shutdown(worker);
        this.channel = null;
        this.boss = null;
        this.worker = null;
        log.info("仓管看板 WebSocket 已关闭");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    public int localPort() {
        Channel current = this.channel;
        if (current == null) {
            return -1;
        }
        if (current.localAddress() instanceof InetSocketAddress address) {
            return address.getPort();
        }
        return -1;
    }
}

package com.allst.netty.tcp;

import com.allst.netty.protocol.WarehouseFrameEncoder;
import com.allst.netty.protocol.WarehouseMessageDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.net.InetSocketAddress;

/**
 * 设备 TCP 服务的启动和关闭。phase 小于模拟客户端，保证先监听再让设备来连。
 * 时序见 docs/08-生命周期.md。
 */
public final class NettyTcpServerLifecycle implements SmartLifecycle {

    public static final int PHASE = 0;

    private static final Logger log = LoggerFactory.getLogger(NettyTcpServerLifecycle.class);

    private final NettyServerProperties properties;
    private final WarehouseFrameEncoder encoder;
    private final WarehouseMessageDecoder decoder;
    private final WarehouseServerHandler handler;

    private volatile boolean running;
    private EventExecutorGroup boss;
    private EventExecutorGroup worker;
    private EventExecutorGroup business;
    private Channel channel;

    public NettyTcpServerLifecycle(NettyServerProperties properties,
                                   WarehouseFrameEncoder encoder,
                                   WarehouseMessageDecoder decoder,
                                   WarehouseServerHandler handler) {
        this.properties = properties;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(properties.getBossThreads(), new DefaultThreadFactory("warehouse-boss"));
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(properties.getWorkerThreads(), new DefaultThreadFactory("warehouse-worker"));
        DefaultEventExecutorGroup businessGroup = new DefaultEventExecutorGroup(resolveBusinessThreads(), new DefaultThreadFactory("warehouse-biz"));
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, properties.getSoBacklog())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 64 * 1024))
                    .childHandler(new WarehouseServerInitializer(properties, encoder, decoder, handler, businessGroup));
            Channel bound = bootstrap.bind(properties.getHost(), properties.getPort()).syncUninterruptibly().channel();
            this.boss = bossGroup;
            this.worker = workerGroup;
            this.business = businessGroup;
            this.channel = bound;
            this.running = true;
            log.info("仓储 TCP 服务已监听 {}", bound.localAddress());
        } catch (RuntimeException ex) {
            NettyThreads.shutdown(bossGroup);
            NettyThreads.shutdown(workerGroup);
            NettyThreads.shutdown(businessGroup);
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
        NettyThreads.shutdown(business);
        this.channel = null;
        this.boss = null;
        this.worker = null;
        this.business = null;
        log.info("仓储 TCP 服务已关闭");
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

    private int resolveBusinessThreads() {
        if (properties.getBusinessThreads() > 0) {
            return properties.getBusinessThreads();
        }
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }
}

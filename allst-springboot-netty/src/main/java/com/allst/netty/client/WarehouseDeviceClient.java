package com.allst.netty.client;

import com.allst.netty.protocol.WarehouseFrameEncoder;
import com.allst.netty.protocol.WarehouseMessage;
import com.allst.netty.protocol.WarehouseMessageDecoder;
import com.allst.netty.tcp.NettyThreads;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 一台扫码枪的长连接。断线后按固定间隔重连，并在每次连上后重新 LOGIN。
 */
public final class WarehouseDeviceClient {

    private static final Logger log = LoggerFactory.getLogger(WarehouseDeviceClient.class);

    private final String host;
    private final int port;
    private final String deviceId;
    private final int heartbeatSeconds;
    private final int reconnectSeconds;
    private final int maxFrameLength;
    private final EventLoopGroup group;
    private final boolean ownsGroup;
    private final WarehouseFrameEncoder encoder;
    private final WarehouseMessageDecoder decoder;
    private final Consumer<WarehouseMessage> commandListener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connectInFlight = new AtomicBoolean(false);

    private volatile Channel channel;
    private volatile ScheduledFuture<?> heartbeat;

    public WarehouseDeviceClient(String host,
                                 int port,
                                 String deviceId,
                                 int heartbeatSeconds,
                                 int reconnectSeconds,
                                 int maxFrameLength,
                                 EventLoopGroup group,
                                 boolean ownsGroup,
                                 WarehouseFrameEncoder encoder,
                                 WarehouseMessageDecoder decoder,
                                 Consumer<WarehouseMessage> commandListener) {
        this.host = host;
        this.port = port;
        this.deviceId = deviceId;
        this.heartbeatSeconds = heartbeatSeconds;
        this.reconnectSeconds = reconnectSeconds;
        this.maxFrameLength = maxFrameLength;
        this.group = group;
        this.ownsGroup = ownsGroup;
        this.encoder = encoder;
        this.decoder = decoder;
        this.commandListener = commandListener;
    }

    public String deviceId() {
        return deviceId;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        connectInFlight.set(true);
        doConnect();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        ScheduledFuture<?> task = heartbeat;
        if (task != null) {
            task.cancel(false);
        }
        Channel current = channel;
        if (current != null) {
            current.close().awaitUninterruptibly(5, TimeUnit.SECONDS);
        }
        if (ownsGroup) {
            NettyThreads.shutdown(group);
        }
    }

    public boolean isActive() {
        Channel current = channel;
        return current != null && current.isActive();
    }

    public ChannelFuture send(WarehouseMessage message) {
        Channel current = channel;
        if (current == null || !current.isActive()) {
            throw new IllegalStateException("device " + deviceId + " is not connected");
        }
        return current.writeAndFlush(message);
    }

    private void doConnect() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .handler(new WarehouseClientInitializer(deviceId, maxFrameLength, encoder, decoder, commandListener));
        try {
            bootstrap.connect(host, port).addListener((ChannelFutureListener) this::onConnectFinished);
        } catch (RuntimeException ex) {
            connectInFlight.set(false);
            log.warn("设备 {} 发起连接失败: {}", deviceId, ex.toString());
            scheduleReconnect();
        }
    }

    private void onConnectFinished(ChannelFuture future) {
        connectInFlight.set(false);
        if (!running.get()) {
            if (future.isSuccess()) {
                future.channel().close();
            }
            return;
        }
        if (!future.isSuccess()) {
            log.warn("设备 {} 连接 {}:{} 失败，{} 秒后重试: {}", deviceId, host, port, reconnectSeconds, future.cause().toString());
            scheduleReconnect();
            return;
        }
        onConnected(future.channel());
    }

    private void onConnected(Channel connected) {
        if (!running.get()) {
            connected.close();
            return;
        }
        this.channel = connected;
        ScheduledFuture<?> task = connected.eventLoop().scheduleAtFixedRate(() -> {
            if (connected.isActive()) {
                connected.writeAndFlush(WarehouseMessage.heartbeat(deviceId));
            }
        }, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
        this.heartbeat = task;
        connected.closeFuture().addListener(future -> {
            task.cancel(false);
            onDisconnected(connected);
        });
        connected.writeAndFlush(WarehouseMessage.login(deviceId));
        log.info("设备 {} 已连接 {}:{} 并发送 LOGIN", deviceId, host, port);
    }

    private void onDisconnected(Channel disconnected) {
        if (channel == disconnected) {
            channel = null;
        }
        log.info("设备 {} 连接断开", deviceId);
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (!running.get() || isActive()) {
            return;
        }
        if (!connectInFlight.compareAndSet(false, true)) {
            return;
        }
        group.schedule(() -> {
            if (!running.get() || isActive()) {
                connectInFlight.set(false);
                return;
            }
            doConnect();
        }, reconnectSeconds, TimeUnit.SECONDS);
    }
}

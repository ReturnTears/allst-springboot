package com.allst.netty.tcp;

import com.allst.netty.protocol.WarehouseFrameDecoder;
import com.allst.netty.protocol.WarehouseFrameEncoder;
import com.allst.netty.protocol.WarehouseMessageDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * 每接受一条设备连接就装一条 Pipeline。解码留在 I/O 线程，业务 Handler 放到独立线程组。
 */
public final class WarehouseServerInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyServerProperties properties;
    private final WarehouseFrameEncoder encoder;
    private final WarehouseMessageDecoder decoder;
    private final WarehouseServerHandler handler;
    private final EventExecutorGroup businessGroup;

    public WarehouseServerInitializer(NettyServerProperties properties,
                                      WarehouseFrameEncoder encoder,
                                      WarehouseMessageDecoder decoder,
                                      WarehouseServerHandler handler,
                                      EventExecutorGroup businessGroup) {
        this.properties = properties;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
        this.businessGroup = businessGroup;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("idle", new IdleStateHandler(properties.getReaderIdleSeconds(), 0, 0));
        pipeline.addLast("frame", new WarehouseFrameDecoder(properties.getMaxFrameLength()));
        pipeline.addLast("message", decoder);
        pipeline.addLast("encoder", encoder);
        pipeline.addLast(businessGroup, "warehouse", handler);
    }
}

package com.allst.netty.client;

import com.allst.netty.protocol.WarehouseFrameDecoder;
import com.allst.netty.protocol.WarehouseFrameEncoder;
import com.allst.netty.protocol.WarehouseMessage;
import com.allst.netty.protocol.WarehouseMessageDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.function.Consumer;

/**
 * 客户端 Pipeline。每次拨号新建一个，避免 ChannelInitializer 被重复挂到 Pipeline 上。
 */
public final class WarehouseClientInitializer extends ChannelInitializer<SocketChannel> {

    private final String deviceId;
    private final int maxFrameLength;
    private final WarehouseFrameEncoder encoder;
    private final WarehouseMessageDecoder decoder;
    private final Consumer<WarehouseMessage> commandListener;

    public WarehouseClientInitializer(String deviceId,
                                      int maxFrameLength,
                                      WarehouseFrameEncoder encoder,
                                      WarehouseMessageDecoder decoder,
                                      Consumer<WarehouseMessage> commandListener) {
        this.deviceId = deviceId;
        this.maxFrameLength = maxFrameLength;
        this.encoder = encoder;
        this.decoder = decoder;
        this.commandListener = commandListener;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("frame", new WarehouseFrameDecoder(maxFrameLength));
        pipeline.addLast("message", decoder);
        pipeline.addLast("encoder", encoder);
        pipeline.addLast("client", new WarehouseClientHandler(deviceId, commandListener));
    }
}

package com.allst.netty.client;

import com.allst.netty.protocol.MessageType;
import com.allst.netty.protocol.WarehouseMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 扫码枪侧的入站处理。收到指令就按原 requestId 回 ACK，表示固件已经接住。
 */
public final class WarehouseClientHandler extends SimpleChannelInboundHandler<WarehouseMessage> {

    private static final Logger log = LoggerFactory.getLogger(WarehouseClientHandler.class);

    private final String deviceId;
    private final Consumer<WarehouseMessage> commandListener;

    public WarehouseClientHandler(String deviceId, Consumer<WarehouseMessage> commandListener) {
        this.deviceId = deviceId;
        this.commandListener = commandListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WarehouseMessage message) {
        if (message.type() != MessageType.COMMAND) {
            log.debug("设备 {} 收到 {} requestId={}", deviceId, message.type(), message.requestId());
            return;
        }
        log.info("设备 {} 收到指令 requestId={} payload={}", deviceId, message.requestId(), message.payload());
        ctx.writeAndFlush(WarehouseMessage.ack(deviceId, message.requestId()));
        if (commandListener == null) {
            return;
        }
        try {
            commandListener.accept(message);
        } catch (RuntimeException ex) {
            log.warn("指令监听器执行失败 deviceId={}", deviceId, ex);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("设备 {} 读包失败: {}", deviceId, cause.toString());
        log.debug("设备读包失败堆栈", cause);
        ctx.close();
    }
}

package com.allst.netty.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 把 {@link WarehouseMessage} 写成一帧。无连接内状态，服务端和全部模拟设备共用一个实例。
 */
@ChannelHandler.Sharable
public final class WarehouseFrameEncoder extends MessageToByteEncoder<WarehouseMessage> {

    private final ObjectMapper objectMapper;

    public WarehouseFrameEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, WarehouseMessage msg, ByteBuf out) throws JsonProcessingException {
        byte[] body = objectMapper.writeValueAsBytes(msg);
        out.writeShort(ProtocolConstants.MAGIC);
        out.writeByte(ProtocolConstants.VERSION);
        out.writeInt(body.length);
        out.writeBytes(body);
    }
}

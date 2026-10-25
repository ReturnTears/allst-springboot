package com.allst.netty.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;

/**
 * 把一整帧还原成 {@link WarehouseMessage}。无连接内状态，可以放进多条 Pipeline。
 */
@ChannelHandler.Sharable
public final class WarehouseMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final ObjectMapper objectMapper;

    public WarehouseMessageDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int magic = in.readUnsignedShort();
        if (magic != ProtocolConstants.MAGIC) {
            throw new CorruptedFrameException("magic mismatch: " + Integer.toHexString(magic));
        }
        int version = in.readUnsignedByte();
        if (version != ProtocolConstants.VERSION) {
            throw new CorruptedFrameException("unsupported version: " + version);
        }
        int length = in.readInt();
        if (length <= 0 || length != in.readableBytes()) {
            throw new CorruptedFrameException("body length mismatch: " + length);
        }
        byte[] body = new byte[length];
        in.readBytes(body);
        try {
            out.add(objectMapper.readValue(body, WarehouseMessage.class));
        } catch (IOException ex) {
            throw new CorruptedFrameException("json body", ex);
        }
    }
}

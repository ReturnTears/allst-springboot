package com.allst.netty.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 按长度字段切出一整帧。每个连接独享一个实例，因为解码器内部有粘包缓存。
 */
public final class WarehouseFrameDecoder extends LengthFieldBasedFrameDecoder {

    public WarehouseFrameDecoder(int maxFrameLength) {
        super(maxFrameLength, ProtocolConstants.LENGTH_FIELD_OFFSET, ProtocolConstants.LENGTH_FIELD_LENGTH, 0, 0);
    }
}

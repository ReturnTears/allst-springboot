package com.allst.netty.protocol;

/**
 * 仓储设备私有帧的固定头部。
 * 布局见 docs/03-自定义协议.md。
 */
public final class ProtocolConstants {

    /** 帧魔数 0xABCD，用来在粘包后把非法字节流尽早丢掉。 */
    public static final int MAGIC = 0xABCD;

    public static final int VERSION = 1;

    /** magic 占 2 字节，version 占 1 字节，length 从这里开始。 */
    public static final int LENGTH_FIELD_OFFSET = 3;

    public static final int LENGTH_FIELD_LENGTH = 4;

    public static final int HEADER_LENGTH = LENGTH_FIELD_OFFSET + LENGTH_FIELD_LENGTH;

    private ProtocolConstants() {
    }
}

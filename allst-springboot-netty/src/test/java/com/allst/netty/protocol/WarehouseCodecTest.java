package com.allst.netty.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.buffer.Unpooled;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void headerLengthMatchesTheFrameLayout() {
        assertEquals(7, ProtocolConstants.HEADER_LENGTH);
    }

    @Test
    void roundTripsTwoFrames() {
        EmbeddedChannel channel = newChannel(1024);
        WarehouseMessage first = WarehouseMessage.scan("WH-SCANNER-01", "SKU-10086", "A-01-03", 2);
        WarehouseMessage second = WarehouseMessage.login("WH-SCANNER-02");
        try {
            assertTrue(channel.writeOutbound(first));
            assertTrue(channel.writeOutbound(second));
            ByteBuf encodedFirst = channel.readOutbound();
            ByteBuf encodedSecond = channel.readOutbound();
            assertEquals(ProtocolConstants.MAGIC, encodedFirst.getUnsignedShort(0));
            assertEquals(ProtocolConstants.VERSION, encodedFirst.getUnsignedByte(2));
            assertTrue(channel.writeInbound(encodedFirst));
            assertTrue(channel.writeInbound(encodedSecond));
            WarehouseMessage decodedFirst = channel.readInbound();
            WarehouseMessage decodedSecond = channel.readInbound();
            assertEquals(MessageType.SCAN, decodedFirst.type());
            assertEquals("SKU-10086", decodedFirst.payload().get("barcode"));
            assertEquals(2, ((Number) decodedFirst.payload().get("quantity")).intValue());
            assertEquals("A-01-03", decodedFirst.payload().get("location"));
            assertEquals(MessageType.LOGIN, decodedSecond.type());
            assertEquals("WH-SCANNER-02", decodedSecond.deviceId());
            assertNull(channel.readInbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void waitsUntilASplitFrameIsComplete() {
        EmbeddedChannel channel = newChannel(1024);
        WarehouseMessage original = WarehouseMessage.scan("WH-SCANNER-01", "SKU-10086", "A-01-03", 1);
        try {
            assertTrue(channel.writeOutbound(original));
            ByteBuf encoded = channel.readOutbound();
            int splitAt = Math.max(1, encoded.readableBytes() / 2);
            ByteBuf head = encoded.readRetainedSlice(splitAt);
            ByteBuf tail = encoded.readRetainedSlice(encoded.readableBytes());
            encoded.release();
            assertFalse(channel.writeInbound(head));
            assertNull(channel.readInbound());
            assertTrue(channel.writeInbound(tail));
            WarehouseMessage decoded = channel.readInbound();
            assertEquals(original.requestId(), decoded.requestId());
            assertEquals("SKU-10086", decoded.payload().get("barcode"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsAFrameWithTheWrongMagic() {
        EmbeddedChannel channel = newChannel(1024);
        ByteBuf bad = Unpooled.buffer();
        bad.writeShort(0x0000);
        bad.writeByte(ProtocolConstants.VERSION);
        bad.writeInt(2);
        bad.writeBytes(new byte[]{'{', '}'});
        try {
            assertThrows(DecoderException.class, () -> {
                channel.writeInbound(bad);
                channel.checkException();
            });
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private EmbeddedChannel newChannel(int maxFrameLength) {
        return new EmbeddedChannel(
                new WarehouseFrameDecoder(maxFrameLength),
                new WarehouseMessageDecoder(objectMapper),
                new WarehouseFrameEncoder(objectMapper)
        );
    }
}

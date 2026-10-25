package com.allst.netty.session;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceSessionRegistryTest {

    @Test
    void secondLoginReplacesThePreviousChannel() {
        DeviceSessionRegistry registry = new DeviceSessionRegistry();
        EmbeddedChannel first = new EmbeddedChannel();
        EmbeddedChannel second = new EmbeddedChannel();
        try {
            registry.bind("WH-SCANNER-01", first);
            registry.bind("WH-SCANNER-01", second);
            assertFalse(first.isActive());
            assertSame(second, registry.find("WH-SCANNER-01").orElseThrow().channel());
            assertFalse(registry.unbindChannel(first));
            assertTrue(registry.unbindChannel(second));
            assertTrue(registry.find("WH-SCANNER-01").isEmpty());
        } finally {
            first.finishAndReleaseAll();
            second.finishAndReleaseAll();
        }
    }

    @Test
    void listsDevicesById() {
        DeviceSessionRegistry registry = new DeviceSessionRegistry();
        EmbeddedChannel low = new EmbeddedChannel();
        EmbeddedChannel high = new EmbeddedChannel();
        try {
            registry.bind("WH-SCANNER-02", high);
            registry.bind("WH-SCANNER-01", low);
            assertEquals("WH-SCANNER-01", registry.list().get(0).deviceId());
            assertEquals("WH-SCANNER-02", registry.list().get(1).deviceId());
        } finally {
            low.finishAndReleaseAll();
            high.finishAndReleaseAll();
        }
    }
}

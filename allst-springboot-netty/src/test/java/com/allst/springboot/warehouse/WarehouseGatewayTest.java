package com.allst.springboot.warehouse;

import com.allst.netty.client.WarehouseDeviceClient;
import com.allst.netty.protocol.WarehouseFrameEncoder;
import com.allst.netty.protocol.WarehouseMessage;
import com.allst.netty.protocol.WarehouseMessageDecoder;
import com.allst.netty.session.CommandRecord;
import com.allst.netty.session.CommandStatus;
import com.allst.netty.session.DeviceView;
import com.allst.netty.session.ScanEvent;
import com.allst.netty.tcp.NettyTcpServerLifecycle;
import com.allst.netty.websocket.NettyWebSocketServerLifecycle;
import com.allst.springboot.warehouse.api.ApiError;
import com.allst.springboot.warehouse.api.GatewayMeta;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "netty.server.port=0",
                "netty.websocket.port=0",
                "netty.client.simulator-enabled=false",
                "netty.server.reader-idle-seconds=120"
        }
)
@AutoConfigureWebTestClient
class WarehouseGatewayTest {

    private static final String DEVICE_ID = "IT-SCANNER-01";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private NettyTcpServerLifecycle tcpServer;

    @Autowired
    private NettyWebSocketServerLifecycle webSocketServer;

    @Autowired
    private WarehouseFrameEncoder encoder;

    @Autowired
    private WarehouseMessageDecoder decoder;

    @Test
    void deviceCanLoginScanAndAckACommand() throws Exception {
        GatewayMeta meta = webTestClient.get().uri("/api/meta")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GatewayMeta.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(meta);
        assertEquals(tcpServer.localPort(), meta.tcpPort());
        assertEquals(webSocketServer.localPort(), meta.websocketPort());
        assertFalse(meta.simulatorEnabled());
        assertTrue(meta.tcpPort() > 0);
        assertTrue(meta.websocketPort() > 0);

        webTestClient.post().uri("/api/devices/{id}/commands", "missing-device")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("action", "MOVE", "params", Map.of("aisle", "A-03")))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiError.class)
                .value(error -> assertEquals("DEVICE_OFFLINE", error.code()));

        webTestClient.post().uri("/api/devices/{id}/commands", DEVICE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("action", " "))
                .exchange()
                .expectStatus().isBadRequest();

        AtomicReference<WarehouseMessage> command = new AtomicReference<>();
        NioEventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("it-device"));
        WarehouseDeviceClient client = new WarehouseDeviceClient(
                "127.0.0.1",
                tcpServer.localPort(),
                DEVICE_ID,
                30,
                1,
                65_536,
                group,
                true,
                encoder,
                decoder,
                command::set
        );
        HttpClient httpClient = HttpClient.newHttpClient();
        CollectingListener listener = new CollectingListener();
        try {
            client.start();
            awaitTrue("device did not appear in /api/devices", () -> devices().stream().anyMatch(view -> DEVICE_ID.equals(view.deviceId()) && view.active()));

            httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create("ws://127.0.0.1:" + webSocketServer.localPort() + "/ws/dashboard"), listener)
                    .get(5, TimeUnit.SECONDS);
            awaitTrue("dashboard snapshot missing device: " + listener.messages, () -> listener.contains("SNAPSHOT") && listener.contains(DEVICE_ID));

            client.send(WarehouseMessage.scan(DEVICE_ID, "IT-SKU-1", "B-02-08", 4)).syncUninterruptibly();
            awaitTrue("scan missing from /api/events", () -> events().stream().anyMatch(event -> "IT-SKU-1".equals(event.barcode()) && event.quantity() == 4));
            awaitTrue("scan missing from websocket: " + listener.messages, () -> listener.contains("\"type\":\"SCAN\"") && listener.contains("IT-SKU-1"));

            CommandRecord accepted = webTestClient.post().uri("/api/devices/{id}/commands", DEVICE_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("action", "MOVE", "params", Map.of("aisle", "A-03")))
                    .exchange()
                    .expectStatus().isAccepted()
                    .expectBody(CommandRecord.class)
                    .returnResult()
                    .getResponseBody();
            assertNotNull(accepted);
            assertEquals(CommandStatus.SENT, accepted.status());

            awaitTrue("device did not observe COMMAND", () -> command.get() != null && accepted.requestId().equals(command.get().requestId()));
            awaitTrue("command was not acked", () -> {
                CommandRecord current = webTestClient.get().uri("/api/commands/{id}", accepted.requestId())
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(CommandRecord.class)
                        .returnResult()
                        .getResponseBody();
                return current != null && current.status() == CommandStatus.ACKED;
            });
            awaitTrue("ack missing from websocket: " + listener.messages, () -> listener.contains("COMMAND_ACK") && listener.contains(accepted.requestId()));
        } finally {
            listener.close();
            client.stop();
        }
    }

    private List<DeviceView> devices() {
        List<DeviceView> body = webTestClient.get().uri("/api/devices")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DeviceView.class)
                .returnResult()
                .getResponseBody();
        return body == null ? List.of() : body;
    }

    private List<ScanEvent> events() {
        List<ScanEvent> body = webTestClient.get().uri("/api/events?limit=50")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ScanEvent.class)
                .returnResult()
                .getResponseBody();
        return body == null ? List.of() : body;
    }

    private static void awaitTrue(String description, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        fail(description);
    }

    private static final class CollectingListener implements WebSocket.Listener {

        private final List<String> messages = new ArrayList<>();
        private final StringBuilder partial = new StringBuilder();
        private volatile WebSocket socket;

        @Override
        public void onOpen(WebSocket webSocket) {
            this.socket = webSocket;
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                synchronized (messages) {
                    messages.add(partial.toString());
                }
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        private boolean contains(String token) {
            synchronized (messages) {
                return messages.stream().anyMatch(message -> message.contains(token));
            }
        }

        private void close() {
            WebSocket current = socket;
            if (current != null) {
                current.sendClose(WebSocket.NORMAL_CLOSURE, "bye").toCompletableFuture().orTimeout(2, TimeUnit.SECONDS).exceptionally(ex -> null).join();
            }
        }
    }
}

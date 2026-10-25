package com.allst.springboot.warehouse.api;

import com.allst.netty.client.DeviceSimulator;
import com.allst.netty.client.SimulatorDisabledException;
import com.allst.netty.protocol.WarehouseMessage;
import com.allst.netty.session.CommandRecord;
import com.allst.netty.session.CommandTracker;
import com.allst.netty.session.DeviceBusyException;
import com.allst.netty.session.DeviceOfflineException;
import com.allst.netty.session.DeviceSession;
import com.allst.netty.session.DeviceSessionRegistry;
import com.allst.netty.session.DeviceView;
import com.allst.netty.session.RecentEventLog;
import com.allst.netty.session.ScanEvent;
import com.allst.netty.push.DashboardFrame;
import com.allst.netty.push.RealtimePushService;
import com.allst.netty.tcp.NettyTcpServerLifecycle;
import com.allst.netty.websocket.NettyWebSocketProperties;
import com.allst.netty.websocket.NettyWebSocketServerLifecycle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * WMS 和仓管看板使用的 HTTP 接口。下发指令是异步的，接口返回时设备可能还没回 ACK。
 */
@Validated
@RestController
@RequestMapping("/api")
public class DeviceConsoleController {

    private final Environment environment;
    private final DeviceSessionRegistry sessions;
    private final RecentEventLog events;
    private final CommandTracker commands;
    private final RealtimePushService push;
    private final NettyWebSocketProperties webSocketProperties;
    private final ObjectProvider<NettyTcpServerLifecycle> tcpServer;
    private final ObjectProvider<NettyWebSocketServerLifecycle> webSocketServer;
    private final ObjectProvider<DeviceSimulator> simulator;

    public DeviceConsoleController(Environment environment,
                                   DeviceSessionRegistry sessions,
                                   RecentEventLog events,
                                   CommandTracker commands,
                                   RealtimePushService push,
                                   NettyWebSocketProperties webSocketProperties,
                                   ObjectProvider<NettyTcpServerLifecycle> tcpServer,
                                   ObjectProvider<NettyWebSocketServerLifecycle> webSocketServer,
                                   ObjectProvider<DeviceSimulator> simulator) {
        this.environment = environment;
        this.sessions = sessions;
        this.events = events;
        this.commands = commands;
        this.push = push;
        this.webSocketProperties = webSocketProperties;
        this.tcpServer = tcpServer;
        this.webSocketServer = webSocketServer;
        this.simulator = simulator;
    }

    @GetMapping("/meta")
    public GatewayMeta meta() {
        return new GatewayMeta(
                environment.getProperty("local.server.port", Integer.class, 8121),
                portOf(tcpServer.getIfAvailable()),
                portOf(webSocketServer.getIfAvailable()),
                webSocketProperties.getPath(),
                simulator.getIfAvailable() != null
        );
    }

    @GetMapping("/devices")
    public java.util.List<DeviceView> devices() {
        return sessions.list();
    }

    @GetMapping("/devices/{deviceId}")
    public DeviceView device(@PathVariable String deviceId) {
        return sessions.find(deviceId)
                .map(DeviceView::from)
                .orElseThrow(() -> new DeviceOfflineException(deviceId));
    }

    @PostMapping("/devices/{deviceId}/commands")
    public ResponseEntity<CommandRecord> command(@PathVariable String deviceId, @Valid @RequestBody CommandRequest request) {
        DeviceSession session = sessions.find(deviceId)
                .filter(DeviceSession::active)
                .orElseThrow(() -> new DeviceOfflineException(deviceId));
        if (!session.channel().isWritable()) {
            throw new DeviceBusyException(deviceId);
        }
        Map<String, Object> params = request.params() == null ? Map.of() : request.params();
        WarehouseMessage message = WarehouseMessage.command(deviceId, request.action().trim(), params);
        CommandRecord record = commands.track(message.requestId(), deviceId, request.action().trim(), params);
        push.commandSent(record);
        session.channel().writeAndFlush(message).addListener(future -> {
            if (!future.isSuccess()) {
                commands.markFailed(message.requestId()).ifPresent(push::commandFailed);
            }
        });
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(record);
    }

    @GetMapping("/commands/{requestId}")
    public CommandRecord commandStatus(@PathVariable String requestId) {
        return commands.find(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "指令不存在"));
    }

    @GetMapping("/events")
    public java.util.List<ScanEvent> events(@RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return events.latest(limit);
    }

    @GetMapping(path = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<DashboardFrame>> stream() {
        return push.stream().map(frame -> ServerSentEvent.<DashboardFrame>builder()
                .event(frame.type())
                .id(frame.requestId() == null ? Long.toString(frame.timestamp()) : frame.requestId())
                .data(frame)
                .build());
    }

    @PostMapping("/simulator/scans")
    public ResponseEntity<Void> simulateScan(@Valid @RequestBody SimulatorScanRequest request) {
        DeviceSimulator deviceSimulator = simulator.getIfAvailable();
        if (deviceSimulator == null) {
            throw new SimulatorDisabledException();
        }
        int quantity = request.quantity() == null ? 1 : request.quantity();
        deviceSimulator.publishScan(request.deviceId(), request.barcode().trim(), request.location().trim(), quantity);
        return ResponseEntity.accepted().build();
    }

    private static int portOf(Object lifecycle) {
        if (lifecycle instanceof NettyTcpServerLifecycle tcp) {
            return tcp.localPort();
        }
        if (lifecycle instanceof NettyWebSocketServerLifecycle webSocket) {
            return webSocket.localPort();
        }
        return -1;
    }
}

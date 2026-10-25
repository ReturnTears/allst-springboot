package com.allst.netty.push;

import com.allst.netty.session.CommandRecord;
import com.allst.netty.session.DeviceSessionRegistry;
import com.allst.netty.session.DeviceView;
import com.allst.netty.session.RecentEventLog;
import com.allst.netty.session.ScanEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 把接入层事件同时送给 WebSocket 看板和 WebFlux SSE。
 * TCP 业务线程只调用 publish，不关心有没有浏览器在看。
 */
public final class RealtimePushService {

    private static final Logger log = LoggerFactory.getLogger(RealtimePushService.class);

    private final DeviceSessionRegistry sessions;
    private final RecentEventLog events;
    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<Channel> dashboards = new CopyOnWriteArrayList<>();
    private final Sinks.Many<DashboardFrame> sink = Sinks.many().replay().limit(200);

    public RealtimePushService(DeviceSessionRegistry sessions, RecentEventLog events, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    public void register(Channel channel) {
        dashboards.addIfAbsent(channel);
        channel.closeFuture().addListener(future -> dashboards.remove(channel));
    }

    public void unregister(Channel channel) {
        dashboards.remove(channel);
    }

    public Flux<DashboardFrame> stream() {
        return sink.asFlux();
    }

    public DashboardFrame snapshot() {
        return DashboardFrame.of("SNAPSHOT", null, null, Map.of(
                "devices", sessions.list(),
                "events", events.latest(50)
        ));
    }

    public void deviceOnline(DeviceView view) {
        publish(DashboardFrame.of("ONLINE", view.deviceId(), null, view));
    }

    public void deviceOffline(String deviceId) {
        publish(DashboardFrame.of("OFFLINE", deviceId, null, Map.of("deviceId", deviceId)));
    }

    public void scan(ScanEvent event) {
        publish(DashboardFrame.of("SCAN", event.deviceId(), event.requestId(), event));
    }

    public void commandSent(CommandRecord record) {
        publish(DashboardFrame.of("COMMAND", record.deviceId(), record.requestId(), record));
    }

    public void commandAck(CommandRecord record) {
        publish(DashboardFrame.of("COMMAND_ACK", record.deviceId(), record.requestId(), record));
    }

    public void commandFailed(CommandRecord record) {
        publish(DashboardFrame.of("COMMAND_FAILED", record.deviceId(), record.requestId(), record));
    }

    private void publish(DashboardFrame frame) {
        emit(frame);
        broadcast(frame);
    }

    private void emit(DashboardFrame frame) {
        for (int attempt = 0; attempt < 64; attempt++) {
            Sinks.EmitResult result = sink.tryEmitNext(frame);
            if (result != Sinks.EmitResult.FAIL_NON_SERIALIZED) {
                if (result.isFailure()) {
                    log.debug("SSE 丢弃看板消息 type={} result={}", frame.type(), result);
                }
                return;
            }
            Thread.onSpinWait();
        }
        log.debug("SSE 连续冲突，丢弃看板消息 type={}", frame.type());
    }

    private void broadcast(DashboardFrame frame) {
        final String json;
        try {
            json = objectMapper.writeValueAsString(frame);
        } catch (JsonProcessingException ex) {
            log.warn("看板消息序列化失败 type={}", frame.type(), ex);
            return;
        }
        for (Channel channel : dashboards) {
            if (!channel.isActive()) {
                dashboards.remove(channel);
                continue;
            }
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }
}

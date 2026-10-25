package com.allst.netty.tcp;

import com.allst.netty.protocol.MessageType;
import com.allst.netty.protocol.WarehouseMessage;
import com.allst.netty.push.RealtimePushService;
import com.allst.netty.session.CommandTracker;
import com.allst.netty.session.DeviceAttrs;
import com.allst.netty.session.DeviceSession;
import com.allst.netty.session.DeviceSessionRegistry;
import com.allst.netty.session.DeviceView;
import com.allst.netty.session.RecentEventLog;
import com.allst.netty.session.ScanEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设备报文的业务处理。实例可共享，连接级状态放在 Channel 属性里。
 */
@ChannelHandler.Sharable
public final class WarehouseServerHandler extends SimpleChannelInboundHandler<WarehouseMessage> {

    private static final Logger log = LoggerFactory.getLogger(WarehouseServerHandler.class);

    private final DeviceSessionRegistry sessions;
    private final RecentEventLog events;
    private final CommandTracker commands;
    private final RealtimePushService push;

    public WarehouseServerHandler(DeviceSessionRegistry sessions,
                                  RecentEventLog events,
                                  CommandTracker commands,
                                  RealtimePushService push) {
        this.sessions = sessions;
        this.events = events;
        this.commands = commands;
        this.push = push;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WarehouseMessage message) {
        if (message.type() == null || message.deviceId() == null || message.deviceId().isBlank()) {
            replyError(ctx, message, "type and deviceId are required", true);
            return;
        }
        if (!sameDevice(ctx, message)) {
            replyError(ctx, message, "deviceId does not match this connection", true);
            return;
        }
        switch (message.type()) {
            case LOGIN -> onLogin(ctx, message);
            case HEARTBEAT -> onHeartbeat(ctx, message);
            case SCAN -> onScan(ctx, message);
            case ACK -> onAck(ctx, message);
            case LOGOUT -> onLogout(ctx, message);
            case COMMAND -> replyError(ctx, message, "device cannot send COMMAND", true);
            case ERROR -> log.info("设备报错 deviceId={} reason={}", message.deviceId(), message.payload());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idle && idle.state() == IdleState.READER_IDLE) {
            log.info("设备读空闲，关闭连接 remote={}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String deviceId = ctx.channel().attr(DeviceAttrs.DEVICE_ID).get();
        if (sessions.unbindChannel(ctx.channel())) {
            push.deviceOffline(deviceId);
            log.info("设备离线 deviceId={} remote={}", deviceId, ctx.channel().remoteAddress());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("设备连接异常 remote={} : {}", ctx.channel().remoteAddress(), cause.toString());
        log.debug("设备连接异常堆栈", cause);
        ctx.close();
    }

    private void onLogin(ChannelHandlerContext ctx, WarehouseMessage message) {
        String previousId = ctx.channel().attr(DeviceAttrs.DEVICE_ID).get();
        if (previousId != null && !previousId.equals(message.deviceId()) && sessions.unbindChannel(ctx.channel())) {
            push.deviceOffline(previousId);
        }
        DeviceSession session = sessions.bind(message.deviceId(), ctx.channel());
        session.touch();
        ctx.writeAndFlush(WarehouseMessage.ack(message.deviceId(), message.requestId()));
        push.deviceOnline(DeviceView.from(session));
        log.info("设备登录 deviceId={} remote={}", message.deviceId(), ctx.channel().remoteAddress());
    }

    private void onHeartbeat(ChannelHandlerContext ctx, WarehouseMessage message) {
        DeviceSession session = requireOnline(ctx, message);
        if (session == null) {
            return;
        }
        session.touch();
        ctx.writeAndFlush(WarehouseMessage.ack(message.deviceId(), message.requestId()));
    }

    private void onScan(ChannelHandlerContext ctx, WarehouseMessage message) {
        DeviceSession session = requireOnline(ctx, message);
        if (session == null) {
            return;
        }
        session.touch();
        ScanEvent event = ScanEvent.from(message);
        events.append(event);
        push.scan(event);
        ctx.writeAndFlush(WarehouseMessage.ack(message.deviceId(), message.requestId()));
        log.info("收到扫码 deviceId={} barcode={} location={}", event.deviceId(), event.barcode(), event.location());
    }

    private void onAck(ChannelHandlerContext ctx, WarehouseMessage message) {
        DeviceSession session = requireOnline(ctx, message);
        if (session == null) {
            return;
        }
        session.touch();
        commands.markAcked(message.requestId()).ifPresent(record -> {
            push.commandAck(record);
            log.info("指令已回执 deviceId={} requestId={} action={}", record.deviceId(), record.requestId(), record.action());
        });
    }

    private void onLogout(ChannelHandlerContext ctx, WarehouseMessage message) {
        DeviceSession session = requireOnline(ctx, message);
        if (session == null) {
            return;
        }
        ctx.writeAndFlush(WarehouseMessage.ack(message.deviceId(), message.requestId()))
                .addListener(ChannelFutureListener.CLOSE);
    }

    private DeviceSession requireOnline(ChannelHandlerContext ctx, WarehouseMessage message) {
        DeviceSession session = sessions.find(message.deviceId()).orElse(null);
        if (session == null || session.channel() != ctx.channel()) {
            replyError(ctx, message, "login required", true);
            return null;
        }
        return session;
    }

    private boolean sameDevice(ChannelHandlerContext ctx, WarehouseMessage message) {
        if (message.type() == MessageType.LOGIN) {
            return true;
        }
        String bound = ctx.channel().attr(DeviceAttrs.DEVICE_ID).get();
        return bound == null || bound.equals(message.deviceId());
    }

    private void replyError(ChannelHandlerContext ctx, WarehouseMessage inbound, String reason, boolean close) {
        String deviceId = inbound == null || inbound.deviceId() == null ? "" : inbound.deviceId();
        String requestId = inbound == null ? null : inbound.requestId();
        var future = ctx.writeAndFlush(WarehouseMessage.error(deviceId, requestId, reason));
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}

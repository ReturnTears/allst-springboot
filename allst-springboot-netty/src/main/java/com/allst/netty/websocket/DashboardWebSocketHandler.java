package com.allst.netty.websocket;

import com.allst.netty.push.RealtimePushService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 握手完成后先推一份快照，之后的增量由 {@link RealtimePushService} 广播。
 */
@ChannelHandler.Sharable
public final class DashboardWebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(DashboardWebSocketHandler.class);

    private final RealtimePushService push;
    private final ObjectMapper objectMapper;

    public DashboardWebSocketHandler(RealtimePushService push, ObjectMapper objectMapper) {
        this.push = push;
        this.objectMapper = objectMapper;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            push.register(ctx.channel());
            writeSnapshot(ctx);
            log.info("看板已接入 remote={}", ctx.channel().remoteAddress());
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        push.unregister(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame text) {
            log.debug("看板上行 {}", text.text());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("看板连接异常 remote={} : {}", ctx.channel().remoteAddress(), cause.toString());
        ctx.close();
    }

    private void writeSnapshot(ChannelHandlerContext ctx) {
        try {
            String json = objectMapper.writeValueAsString(push.snapshot());
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (JsonProcessingException ex) {
            log.warn("看板快照序列化失败", ex);
        }
    }
}

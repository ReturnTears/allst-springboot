package com.allst.netty.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * WebSocket 握手走 HTTP 编解码，握手完成后才把文本帧交给看板 Handler。
 */
public final class DashboardWebSocketInitializer extends ChannelInitializer<SocketChannel> {

    private final String path;
    private final DashboardWebSocketHandler handler;

    public DashboardWebSocketInitializer(String path, DashboardWebSocketHandler handler) {
        this.path = path;
        this.handler = handler;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65_536));
        pipeline.addLast(new WebSocketServerProtocolHandler(path));
        pipeline.addLast(handler);
    }
}

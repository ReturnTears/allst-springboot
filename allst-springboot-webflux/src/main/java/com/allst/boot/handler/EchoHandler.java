package com.allst.boot.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * @author Hutu
 * @since 2024-08-17 下午 10:28
 */
@Component
public class EchoHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(final WebSocketSession session) {
        System.out.println("come in method EchoHandler.handle");
        return session.send(session.receive().map(msg ->
                session.textMessage("Server Return ：Halo ， -> " + msg.getPayloadAsText())
        ));
    }
}

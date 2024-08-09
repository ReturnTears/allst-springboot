package com.allst.boot.event;

import org.springframework.stereotype.Component;

/**
 * @author Hutu
 * @since 2024-08-09 下午 09:42
 */
@Component
public class EmailMessageEventHandler implements MessageEventHandler {
    @Override
    public String handle(MessageEvent event) {
        System.out.println("handle email event......");
        event.onMessage("email message");
        return "email message";
    }
}

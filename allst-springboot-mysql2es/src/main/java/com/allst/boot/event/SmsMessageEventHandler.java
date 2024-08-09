package com.allst.boot.event;

import org.springframework.stereotype.Component;

/**
 * @author Hutu
 * @since 2024-08-09 下午 09:41
 */
@Component
public class SmsMessageEventHandler implements MessageEventHandler {
    @Override
    public String handle(MessageEvent event) {
        System.out.println("handle sms event......");
        event.onMessage("sms message");
        return "sms message";
    }
}

package com.allst.boot.service;

import com.allst.boot.enums.EventEnum;
import com.allst.boot.event.MessageEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * messageEventMap 将会被自动注入，其中的键是实现类的 Bean 名称（默认情况下是实现类的简单类名首字母小写，即 emailMessageEventHandler 和 smsMessageEventHandler）。
 * 如果希望使用不同的键，可以在实现类上使用 @Qualifier 注解来指定。
 *
 * @author Hutu
 * @since 2024-08-09 下午 09:43
 */
@Service
@RequiredArgsConstructor
public class MessageService {
    private final Map<String, MessageEventHandler> messageEventMap;
    public String getMessageEvent(String msg) {
        String handler = EventEnum.getHandler(msg);
        MessageEventHandler eventHandler = messageEventMap.get(handler);
        return eventHandler.handle(message -> {
            System.out.println("Message: " + message);
        });
    }
}

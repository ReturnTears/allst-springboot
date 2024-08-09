package com.allst.boot.event;

/**
 * @author Hutu
 * @since 2024-08-09 下午 09:41
 */
public interface MessageEventHandler {
    String handle(MessageEvent event);
}

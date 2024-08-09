package com.allst.boot.controller;

import com.allst.boot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-08-09 下午 09:45
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/event")
public class EventController {
    private final MessageService messageService;
    @RequestMapping(value = "/get/{msg}")
    public String getEvent(@PathVariable(value = "msg") String msg) {
        return messageService.getMessageEvent(msg);
    }
}

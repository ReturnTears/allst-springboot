package com.allst.springboot.controller;

import com.allst.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.allst.springboot.entity.User;

/**
 * @author Hutu
 * @since 2025-02-26 下午 11:18
 */
@RestController
@RequestMapping("/sse")
public class SSEController {

    @Autowired
    private UserService userService;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/get", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getAllUsers() {
        SseEmitter emitter = new SseEmitter();
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onError((e) -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));

        return emitter;
    }

    @GetMapping("/set")
    public void addUser() {
        sendToClients();
    }

    public void sendToClients() {
        List<User> users = userService.selectUsers();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(users);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
}

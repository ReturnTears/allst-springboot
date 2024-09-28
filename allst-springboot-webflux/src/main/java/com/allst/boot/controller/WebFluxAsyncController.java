package com.allst.boot.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@RestController
@RequestMapping("/test")
public class WebFluxAsyncController {
    public String handleWork() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        return "handleWork end";
    }

    @GetMapping("/syn")
    public String syn() {
        System.out.println("syn start : " + LocalDateTime.now());
        String work = handleWork();
        System.out.println("syn end : " + LocalDateTime.now());
        return work;
    }

    @GetMapping("/asyn")
    public Mono<String> asyn() {
        System.out.println("asyn start : " + LocalDateTime.now());
        Mono<String> mono = Mono.fromSupplier(this::handleWork);
        System.out.println("asyn end : " + LocalDateTime.now());
        return mono;
    }

    /**
     * 服务器定期向客户端发送数据
     * 服务器发送事件(Server-Sent Events, SSE)是基于WebSocket协议的一种服务器向客户端发送事件和数据的单向通讯
     */
    @GetMapping(value = "/echo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Date> echo() {
        return Flux.fromStream(
                Stream.generate(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                    return new Date();
                }).limit(5)
        );
    }
}

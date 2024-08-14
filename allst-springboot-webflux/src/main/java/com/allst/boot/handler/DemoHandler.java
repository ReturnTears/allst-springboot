package com.allst.boot.handler;

import com.google.gson.Gson;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * ServerResponse 是对响应的封装，可以设置响应状态、响应头、响应正文。比如 ok 代表的是 200 响应码、MediaType 枚举是代表这文本内容类型、返回的是 String 的对象。
 * @author Hutu
 * @since 2024-08-14 下午 10:32
 */
@Component
public class DemoHandler {
    public Mono<ServerResponse> helloDemo(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromValue("Hello, Demo!"));
    }
}

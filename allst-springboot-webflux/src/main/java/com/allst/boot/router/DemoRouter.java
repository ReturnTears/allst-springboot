package com.allst.boot.router;

import com.allst.boot.handler.DemoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * RouterFunctions 对请求路由处理类，即将请求路由到处理器，这里将一个 GET 请求 /hello 路由到处理器 demoHandler 的 helloDemo 方法上。
 * 跟 Spring MVC 模式下的 HandleMapping 的作用类似。
 *
 * @author Hutu
 * @since 2024-08-14 下午 10:37
 */
@Configuration
public class DemoRouter {
    @Bean
    public RouterFunction<ServerResponse> routeCity(DemoHandler demoHandler) {
        return RouterFunctions.route(RequestPredicates.GET("/demo")
                .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), demoHandler::helloDemo);
    }
}

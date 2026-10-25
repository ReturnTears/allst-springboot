package com.allst.springboot.extension.web;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.web.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * 内嵌 Web 容器定制。不修改当前工程已经使用的端口和上下文路径。
 */
@Component
public class AdvsWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        factory.addInitializers(servletContext -> {
            long initializedAt = System.currentTimeMillis();
            servletContext.setAttribute("advs.startup.initializedAt", initializedAt);
            StartupTraceLog.record(StartupPhase.SERVLET_CONTEXT_INITIALIZER,
                    "ServletContext 已可写，advs.startup.initializedAt=" + initializedAt
                            + "。Boot 2.3 里这一步通常仍发生在 refresh.onRefresh。");
        });
        StartupTraceLog.record(StartupPhase.WEB_SERVER_CUSTOMIZER,
                "已追加 ServletContextInitializer。未修改 server.port 与 context-path。");
    }
}

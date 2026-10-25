package com.allst.springboot.extension.bootstrap;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 在配置文件加载之后、Bean 创建之前补齐本模块的默认项。
 * order 取最低优先级，保证 application.properties 和 beta 配置先被加载。
 */
public class AdvsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE = "advs-extension-defaults";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("advs.extension.cache-warmup", "false");
        defaults.put("advs.extension.fail-fast", "false");
        defaults.put("advs.extension.banner", "ALLST Spring Extension Points");
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE, defaults));

        if (environment.getProperty("advs.extension.fail-fast", Boolean.class, Boolean.FALSE)) {
            String token = environment.getProperty("advs.extension.required-token");
            if (token == null || token.isBlank()) {
                throw new AdvsStartupException(
                        "已开启 advs.extension.fail-fast，但未配置 advs.extension.required-token");
            }
        }

        String profiles = environment.getActiveProfiles().length == 0
                ? "(无)"
                : String.join(",", environment.getActiveProfiles());
        StartupTraceLog.record(StartupPhase.ENVIRONMENT_POST_PROCESSOR,
                "默认配置已用 addLast 补齐，不覆盖已有值。profiles=" + profiles
                        + ", port=" + environment.getProperty("server.port")
                        + ", contextPath=" + environment.getProperty("server.servlet.context-path")
                        + ", cacheWarmup=" + environment.getProperty("advs.extension.cache-warmup")
                        + ", banner=" + environment.getProperty("advs.extension.banner"));
    }
}

package com.allst.exercise.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson 定制：响应体中值为 null 或「空」的属性在序列化时移除。
 * <p>
 * {@link JsonInclude.Include#NON_EMPTY} 的「空」包括：null、空字符串 ""、
 * 空集合/空 Map、空数组。比 NON_NULL 更彻底，满足「值为空或 null 就移除」的要求。
 * <p>
 * 用 {@link Jackson2ObjectMapperBuilderCustomizer} 而不是直接 new ObjectMapper，
 * 是为了保留 Spring Boot 自动配置的模块（如 JavaTimeModule）与 application.properties
 * 里的 date-format / time-zone 设置，只叠加序列化包含策略。
 *
 * @author Hutu
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer nonEmptyCustomizer() {
        return builder -> builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
    }
}
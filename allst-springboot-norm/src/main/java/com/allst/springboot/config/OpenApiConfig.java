package com.allst.springboot.config;

import com.allst.springboot.annotation.Idempotent;
import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.header.HeaderNames;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

/**
 * OpenAPI / Knife4j。公共请求头按接口是否签名、是否幂等标记为必填。
 * 对应规范：14 接口文档，16 请求头。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI normOpenApi() {
        return new OpenAPI().info(new Info()
                .title("allst-springboot-norm 接口规范")
                .version("1.0.0")
                .description("签名、加密、IP 白名单、限流、参数校验、统一返回、统一异常、请求日志、幂等、"
                        + "条数限制、异步、脱敏、批量操作与职责拆分的可运行示例。详细说明见项目 docs 目录。"));
    }

    @Bean
    public OperationCustomizer normHeaderCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            boolean sign = AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequireSign.class)
                    || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequireSign.class);
            boolean idempotent = AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), Idempotent.class);
            operation.addParametersItem(header(HeaderNames.TRACE_ID, "链路追踪号。不传则服务端生成，并在响应头回传。不参与签名。", false));
            operation.addParametersItem(header(HeaderNames.OPERATOR, "操作人，写入 createdBy。参与签名。", false));
            operation.addParametersItem(header(HeaderNames.APP_ID, "调用方应用标识。", sign));
            operation.addParametersItem(header(HeaderNames.TIMESTAMP, "Unix 毫秒时间戳。", sign));
            operation.addParametersItem(header(HeaderNames.NONCE, "一次性随机串，窗口期内不可重复。", sign));
            operation.addParametersItem(header(HeaderNames.SIGN, "HMAC-SHA256 十六进制小写签名。", sign));
            if (idempotent) {
                operation.addParametersItem(header(HeaderNames.IDEMPOTENT_KEY, "幂等键，8 到 64 位字母、数字、下划线或中划线。参与签名。", true));
            }
            return operation;
        };
    }

    private Parameter header(String name, String description, boolean required) {
        return new Parameter()
                .name(name)
                .in("header")
                .description(description)
                .required(required)
                .schema(new StringSchema());
    }
}

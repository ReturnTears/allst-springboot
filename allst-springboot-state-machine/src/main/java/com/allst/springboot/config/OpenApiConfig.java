package com.allst.springboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stateMachineOpenApi() {
        return new OpenAPI().info(new Info()
                .title("状态机规范示例")
                .version("1.0")
                .description("订单履约、合同并行审核、发票开具。细节见项目 docs 目录。"));
    }
}

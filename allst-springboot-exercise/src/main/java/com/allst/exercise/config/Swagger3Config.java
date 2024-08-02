package com.allst.exercise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Hutu
 * @since 2024-08-02 下午 09:47
 */
@Configuration
@EnableSwagger2
public class Swagger3Config {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .enable(true)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.allst.exercise.controller"))
            .paths(PathSelectors.any())
            .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("Spring Boot中使用Swagger2构建RESTful APIs")
            .description("测试")
            .termsOfServiceUrl("https://www.baidu.com/")
            .contact(new Contact("hu", "https://www.baidu.com/", "709844757@qq.com"))
            .version("1.0")
            .build();
    }
}

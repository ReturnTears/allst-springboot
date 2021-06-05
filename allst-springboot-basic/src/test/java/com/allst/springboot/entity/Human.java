package com.allst.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

/**
 * @author YiYa
 * @since 2020-08-05 下午 10:54
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Component
@PropertySource("classpath:human.properties")
@ConfigurationProperties(prefix = "human")
@Validated // 开启数据校验
public class Human {
    private String name;
    private Integer age;
    private String address;
    private String msg;
    @NotNull
    private String email;
}

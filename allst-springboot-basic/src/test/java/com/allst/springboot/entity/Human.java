package com.allst.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

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
public class Human {
    private String name;
    private Integer age;
    private String address;
    private String msg;
}

package com.allst.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author YiYa
 * @since 2020-08-05 上午 12:06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Component
public class People {

    @Value("#{person.name}")
    private String name;
    @Value("#{person.age}")
    private Integer age;
    @Value("#{person.age + 10}")
    private Integer tenAge;
}

package com.allst.exercise.model;

import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Hutu
 * @since 2024-07-28 下午 02:16
 */
@Data
@Table("person")
public class Person {
    private Long id;
    private String firstName;
    private String lastName;
    private Integer age;
}

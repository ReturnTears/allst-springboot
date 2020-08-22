package com.allst.springboot.bean;

import lombok.Data;

/**
 * @author YiYa
 * @since 2020-08-23 上午 01:03
 */
@Data
public class Employee {
    private Integer id;
    private String lastName;
    private String email;
    private Integer gender;
    private Integer dId;
}

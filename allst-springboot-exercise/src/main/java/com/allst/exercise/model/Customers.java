package com.allst.exercise.model;

import lombok.Data;

/**
 * @author Hutu
 * @since 2024-07-27 下午 11:38
 */
@Data
public class Customers {
    private Integer id;
    private String name;
    private String email;
    private String address;
    private String city;
    private String country;
}

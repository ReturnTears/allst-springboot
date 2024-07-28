package com.allst.exercise.model;

import com.allst.exercise.anno.EmailFormat;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Hutu
 * @since 2024-07-27 下午 11:38
 */
@Data
@Table("customers")
public class Customers {
    //@Id(keyType = KeyType.Auto)
    private Long id;
    private String name;
    @EmailFormat
    private String email;
    private String address;
    private String city;
    private String country;
}

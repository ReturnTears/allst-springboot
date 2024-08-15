package com.allst.boot.domain;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author Hutu
 * @since 2024-08-15 下午 11:06
 */
@Data
@Document("lagou")
public class Lagou {
    private String id;
    private String name;
    private Date birthday;
    private Double salary;
    private String city;
}

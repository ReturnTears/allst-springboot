package com.allst.springboot.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Hutu
 * @since 2024-09-08 下午 09:40
 */
@Data
public class BookVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;

    private String name;

    private String author;

    // 1-上架，2-在管，3-借出，4-损坏，5-丢失，6-下架
    private String status;

    // 字段添加填充内容
    private Date createTime;

    private Date updateTime;
}

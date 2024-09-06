package com.allst.springboot.pojo;

import com.allst.springboot.enums.BookStatusEnum;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Hutu
 * @since 2024-09-06 下午 11:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("book")
public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String author;
    // 1-上架，2-在管，3-借出，4-损坏，5-丢失，6-下架
    private BookStatusEnum status;

    // 字段添加填充内容
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}

package com.allst.springboot.pojo;

import com.allst.springboot.enums.BookStatusEnum;
import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

/**
 * @author Hutu
 * @since 2024-09-06 下午 11:16
 */
@TableName("book")
public class Book {
    @TableId(type = IdType.AUTO)
    @TableField(value = "id")
    private Long id;

    @TableField(value = "name")
    private String name;

    @TableField(value = "author")
    private String author;

    // 1-上架，2-在管，3-借出，4-损坏，5-丢失，6-下架
    @TableField(value = "status")
    private BookStatusEnum status;

    // 字段添加填充内容
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BookStatusEnum getStatus() {
        return status;
    }

    public void setStatus(BookStatusEnum status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

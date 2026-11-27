package com.allst.springboot.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Hutu
 * @since 2024-09-08 下午 09:40
 */
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

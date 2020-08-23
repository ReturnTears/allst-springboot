package com.allst.springboot.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * 使用缓存的对象实体类需要实现Serializable序列化接口
 * @author YiYa
 * @since 2020-08-23 上午 01:07
 */
@Data
public class Department implements Serializable {
    private Integer id;
    private String departmentName;
    private Integer employeeNums;
}

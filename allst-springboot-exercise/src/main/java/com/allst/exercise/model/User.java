package com.allst.exercise.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Hutu
 * @since 2024-08-02 下午 09:57
 */
public class User {
    /**
     * 编号
     */
    @ApiModelProperty(value = "主键", example = "1")
    private Long id;
    /**
     * 姓名
     */
    @ApiModelProperty(value = "姓名", example = "kang")
    private String name;

    /**
     * 年龄
     */
    @ApiModelProperty(value = "年龄", example = "18")
    private Integer age;

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}

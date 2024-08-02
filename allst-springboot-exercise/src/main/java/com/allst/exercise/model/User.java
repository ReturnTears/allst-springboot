package com.allst.exercise.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Hutu
 * @since 2024-08-02 下午 09:57
 */
@Data
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
}

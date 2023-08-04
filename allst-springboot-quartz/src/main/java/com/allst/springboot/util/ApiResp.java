package com.allst.springboot.util;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Hutu
 * @since 2023-05-30 下午 10:02
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResp<T> implements Serializable {
    @ApiModelProperty(value = "状态，0-成功，1-失败")
    private int code;

    @ApiModelProperty(value = "信息")
    private String msg;

    @ApiModelProperty(value = "数据体")
    private T data;

    /**
     * 失败，不带错误信息
     */
    public static ApiResp<Object> error(String msg) {
        return new ApiResp<>(1, msg, null);
    }

    /**
     * 失败，不带错误信息
     */
    public static ApiResp<Object> error() {
        return new ApiResp<>(1, null, null);
    }

    /**
     * 成功，不带返回数据
     */
    public static ApiResp<Object> success(String msg) {
        return new ApiResp<>(0, msg, null);
    }

    /**
     * 成功，不带返回数据
     */
    public static ApiResp<Object> success() {
        return new ApiResp<>(0, "请求成功", null);
    }
}

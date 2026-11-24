package com.allst.exercise.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应体。所有控制器返回都包一层 {@code Result}，前端按固定结构解析。
 * <p>
 * 空值剔除由 {@link com.allst.exercise.config.JacksonConfig} 全局配置
 * （{@code JsonInclude.Include.NON_EMPTY}）完成：值为 null 或空字符串/空集合的属性
 * 在序列化时直接移除，不出现在 JSON 里。因此本类无需任何 Jackson 注解。
 *
 * @param <T> 业务数据类型
 */
public class Result<T> {

    /** 业务码：200 成功，其余为失败。 */
    private Integer code;

    /** 提示信息。 */
    private String message;

    /** 业务数据。为 null 时该属性会被全局配置移除。 */
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
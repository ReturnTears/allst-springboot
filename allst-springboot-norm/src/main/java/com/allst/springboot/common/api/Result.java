package com.allst.springboot.common.api;

import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.common.header.RequestMeta;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一返回体。成功时 code 为 200，失败时 code 为业务码，HTTP 状态另见异常处理。
 * 对应规范：6 返回统一值。
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private String traceId;
    private long timestamp;

    public static <T> Result<T> ok(T data) {
        Result<T> result = base(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode, resultCode.getMessage());
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return base(resultCode.getCode(), message);
    }

    private static <T> Result<T> base(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTraceId(currentTraceId());
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    private static String currentTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }
        RequestMeta meta = RequestContext.get();
        return meta == null ? null : meta.getTraceId();
    }
}

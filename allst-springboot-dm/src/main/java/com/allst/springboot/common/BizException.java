package com.allst.springboot.common;

/**
 * 业务异常。示例里用它区分「规则不允许」和「系统出错」。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

package com.allst.springboot.common.exception;

import com.allst.springboot.common.api.ResultCode;
import lombok.Getter;

/**
 * 可预期的业务失败。消息会返回给调用方，不要把 SQL、密钥或堆栈放进来。
 * 对应规范：7 统一封装异常。
 */
@Getter
public class BizException extends RuntimeException {

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}

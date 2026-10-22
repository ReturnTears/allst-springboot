package com.allst.springboot.redis.common;

/**
 * 可预期的业务失败，例如参数不合法、库存不足、未登录。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}

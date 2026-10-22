package com.allst.springboot.common.api;

/**
 * 业务码。HTTP 状态表达传输结果，这里的数字给调用方区分原因。
 */
public final class ResultCode {

    public static final int OK = 0;
    public static final int VALIDATION = 40000;
    public static final int PASSWORD_POLICY = 40001;
    public static final int DUPLICATE = 40002;
    public static final int UNAUTHORIZED = 40100;
    public static final int BAD_CREDENTIALS = 40101;
    public static final int LOCKED = 40102;
    public static final int DISABLED = 40104;
    public static final int SESSION_EXPIRED = 40105;
    public static final int EXTERNAL_NOT_BOUND = 40106;
    public static final int FORBIDDEN = 40300;
    public static final int PASSWORD_EXPIRED = 40310;
    public static final int NOT_ENABLED = 40400;
    public static final int RATE_LIMIT = 42900;
    public static final int SERVER = 50000;
    public static final int MAINTENANCE = 50301;

    private ResultCode() {
    }
}

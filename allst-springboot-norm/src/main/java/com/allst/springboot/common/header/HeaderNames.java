package com.allst.springboot.common.header;

/**
 * 公共请求头。业务参数放在 Body 或 Query，调用方身份、链路和幂等放在 Header。
 * 对应规范：16 请求头。
 */
public final class HeaderNames {

    public static final String TRACE_ID = "X-Trace-Id";
    public static final String APP_ID = "X-App-Id";
    public static final String TIMESTAMP = "X-Timestamp";
    public static final String NONCE = "X-Nonce";
    public static final String SIGN = "X-Sign";
    public static final String IDEMPOTENT_KEY = "X-Idempotent-Key";
    public static final String OPERATOR = "X-Operator";

    private HeaderNames() {
    }
}

package com.allst.springboot.common.header;

import lombok.Data;

/**
 * 当前请求上解析出的公共头。只在请求线程内有效。
 */
@Data
public class RequestMeta {

    private String traceId;
    private String appId;
    private String timestamp;
    private String nonce;
    private String sign;
    private String idempotentKey;
    private String operator;
    private String clientIp;
    private long requestTime;
    private String method;
    private String path;
}

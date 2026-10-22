package com.allst.springboot.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务码与 HTTP 状态分离：body.code 给调用方分支，HTTP 状态给网关和监控。
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(200, 200, "成功"),
    BAD_REQUEST(400, 400, "请求参数错误"),
    VALIDATION_FAILED(400, 400, "参数校验失败"),
    JSON_INVALID(400, 400, "请求体不是合法的 JSON"),
    PAGE_LIMIT(40010, 400, "分页条数超出限制"),
    BATCH_LIMIT(40011, 400, "批量条数超出限制"),
    METHOD_NOT_ALLOWED(405, 405, "不支持的请求方法"),
    API_NOT_FOUND(404, 404, "接口不存在"),
    NOT_FOUND(40401, 404, "资源不存在"),
    SIGN_MISSING(40100, 401, "缺少签名请求头"),
    SIGN_INVALID(40101, 401, "签名不正确"),
    SIGN_EXPIRED(40102, 401, "请求已过期"),
    SIGN_REPLAY(40103, 401, "随机串已被使用"),
    SIGN_APP(40104, 401, "未知的应用标识"),
    IP_FORBIDDEN(40301, 403, "IP 不在白名单"),
    CONFLICT(40900, 409, "数据冲突"),
    IDEMPOTENT_CONFLICT(40901, 409, "重复请求"),
    USERNAME_EXISTS(40902, 409, "用户名已存在"),
    IDEMPOTENT_PROCESSING(40903, 409, "请求正在处理中"),
    PAYLOAD_TOO_LARGE(41301, 413, "请求体超出大小限制"),
    RATE_LIMIT(42901, 429, "请求过于频繁，请稍后再试"),
    INTERNAL_ERROR(500, 500, "系统异常");

    private final int code;
    private final int httpStatus;
    private final String message;
}

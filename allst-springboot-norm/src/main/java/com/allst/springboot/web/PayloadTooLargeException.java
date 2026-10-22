package com.allst.springboot.web;

/**
 * 请求体超过配置上限。由过滤器转成 413，不把超大报文读进内存。
 */
public class PayloadTooLargeException extends RuntimeException {

    public PayloadTooLargeException() {
        super("payload too large");
    }
}

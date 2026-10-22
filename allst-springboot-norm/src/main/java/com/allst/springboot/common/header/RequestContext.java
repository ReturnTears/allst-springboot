package com.allst.springboot.common.header;

/**
 * 请求上下文。过滤器创建，拦截器补充，请求结束时必须清理，避免线程池复用串数据。
 */
public final class RequestContext {

    private static final ThreadLocal<RequestMeta> HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(RequestMeta meta) {
        HOLDER.set(meta);
    }

    public static RequestMeta get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

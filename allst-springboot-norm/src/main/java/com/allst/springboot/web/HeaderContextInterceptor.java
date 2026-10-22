package com.allst.springboot.web;

import com.allst.springboot.common.header.HeaderNames;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.common.header.RequestMeta;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 解析公共请求头，业务代码从 {@link RequestContext} 读取，不再各处 getHeader。
 * 对应规范：16 请求头。
 */
@Component
public class HeaderContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }
        RequestMeta meta = RequestContext.get();
        if (meta == null) {
            meta = new RequestMeta();
            String traceId = request.getHeader(HeaderNames.TRACE_ID);
            meta.setTraceId(traceId);
            RequestContext.set(meta);
        }
        meta.setAppId(trim(request.getHeader(HeaderNames.APP_ID)));
        meta.setTimestamp(trim(request.getHeader(HeaderNames.TIMESTAMP)));
        meta.setNonce(trim(request.getHeader(HeaderNames.NONCE)));
        meta.setSign(trim(request.getHeader(HeaderNames.SIGN)));
        meta.setIdempotentKey(trim(request.getHeader(HeaderNames.IDEMPOTENT_KEY)));
        meta.setOperator(trim(request.getHeader(HeaderNames.OPERATOR)));
        if (meta.getTraceId() != null && !meta.getTraceId().isBlank()) {
            MDC.put("traceId", meta.getTraceId());
        }
        return true;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

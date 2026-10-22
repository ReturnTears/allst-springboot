package com.allst.springboot.web;

import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.header.RequestMeta;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.config.NormProperties;
import com.allst.springboot.crypto.SignUtils;
import com.allst.springboot.security.NonceStore;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 校验 HMAC 签名、时间窗和随机串。签名失败不消耗随机串，方便调用方修正后重试。
 * 对应规范：1 签名。
 */
@Component
public class SignatureInterceptor implements HandlerInterceptor {

    private final NormProperties properties;
    private final NonceStore nonceStore;

    public SignatureInterceptor(NormProperties properties, NonceStore nonceStore) {
        this.properties = properties;
        this.nonceStore = nonceStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }
        if (!properties.getSign().isEnabled() || !needSign(handler)) {
            return true;
        }
        RequestMeta meta = RequestContext.get();
        String appId = value(meta, meta == null ? null : meta.getAppId());
        String timestamp = value(meta, meta == null ? null : meta.getTimestamp());
        String nonce = value(meta, meta == null ? null : meta.getNonce());
        String sign = value(meta, meta == null ? null : meta.getSign());
        if (appId == null || timestamp == null || nonce == null || sign == null) {
            throw new BizException(ResultCode.SIGN_MISSING);
        }
        String secret = properties.getSign().getApps().get(appId);
        if (secret == null || secret.isBlank()) {
            throw new BizException(ResultCode.SIGN_APP);
        }
        long ts = parseTimestamp(timestamp);
        long skew = Math.abs(System.currentTimeMillis() - ts);
        if (skew > properties.getSign().getExpireSeconds() * 1000L) {
            throw new BizException(ResultCode.SIGN_EXPIRED);
        }
        String body = "";
        if (request instanceof RepeatableReadRequestWrapper) {
            body = ((RepeatableReadRequestWrapper) request).bodyAsString();
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String idempotentKey = meta.getIdempotentKey();
        String operator = meta.getOperator();
        String canonical = SignUtils.canonical(request.getMethod(), path, request.getQueryString(), body,
                appId, timestamp, nonce, idempotentKey, operator);
        String expected = SignUtils.sign(canonical, secret);
        if (!SignUtils.constantTimeEquals(expected, sign.toLowerCase(Locale.ROOT))) {
            throw new BizException(ResultCode.SIGN_INVALID);
        }
        if (!nonceStore.remember(appId, nonce, properties.getSign().getExpireSeconds() * 1000L)) {
            throw new BizException(ResultCode.SIGN_REPLAY);
        }
        return true;
    }

    private long parseTimestamp(String timestamp) {
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new BizException(ResultCode.SIGN_EXPIRED, "时间戳格式不正确");
        }
    }

    private boolean needSign(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        HandlerMethod method = (HandlerMethod) handler;
        return method.hasMethodAnnotation(RequireSign.class)
                || method.getBeanType().isAnnotationPresent(RequireSign.class);
    }

    private String value(RequestMeta meta, String current) {
        return current == null || current.isBlank() ? null : current;
    }
}

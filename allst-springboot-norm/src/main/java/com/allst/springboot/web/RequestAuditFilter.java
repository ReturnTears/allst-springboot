package com.allst.springboot.web;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.api.ResultWriter;
import com.allst.springboot.common.header.HeaderNames;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.common.header.RequestMeta;
import com.allst.springboot.config.NormProperties;
import com.allst.springboot.log.ApiLogService;
import com.allst.springboot.log.ApiRequestLog;
import com.allst.springboot.log.SensitiveTextMasker;
import com.allst.springboot.security.ClientIpResolver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 最外层过滤器：生成链路号、缓存请求体、记录访问日志。
 * 健康检查默认只打应用日志，不落库，避免压测被写日志拖慢。对应规范：8 请求日志。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestAuditFilter extends OncePerRequestFilter {

    private static final String META_ATTRIBUTE = RequestAuditFilter.class.getName() + ".meta";
    private static final String BODY_ATTRIBUTE = RequestAuditFilter.class.getName() + ".body";

    private final NormProperties properties;
    private final ApiLogService apiLogService;

    public RequestAuditFilter(NormProperties properties, ApiLogService apiLogService) {
        this.properties = properties;
        this.apiLogService = apiLogService;
    }

    /**
     * 异步接口会第二次进入过滤器。第一次只缓存上下文，等结果写完再记日志。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            finishAsync(request, response, chain);
            return;
        }
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if (!uri.startsWith(context + "/api/")) {
            chain.doFilter(request, response);
            return;
        }
        String traceId = request.getHeader(HeaderNames.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        response.setHeader(HeaderNames.TRACE_ID, traceId);
        MDC.put("traceId", traceId);
        RequestMeta meta = new RequestMeta();
        meta.setTraceId(traceId);
        meta.setClientIp(ClientIpResolver.resolve(request, properties.getIp().isTrustForwarded()));
        meta.setRequestTime(System.currentTimeMillis());
        meta.setMethod(request.getMethod());
        meta.setPath(uri.substring(context.length()));
        RequestContext.set(meta);
        try {
            int contentLength = request.getContentLength();
            if (contentLength > properties.getHttp().getMaxBodyBytes()) {
                ResultWriter.write(response, HttpStatus.PAYLOAD_TOO_LARGE, Result.fail(ResultCode.PAYLOAD_TOO_LARGE));
                return;
            }
            RepeatableReadRequestWrapper wrappedRequest;
            try {
                wrappedRequest = new RepeatableReadRequestWrapper(request, properties.getHttp().getMaxBodyBytes());
            } catch (PayloadTooLargeException ex) {
                ResultWriter.write(response, HttpStatus.PAYLOAD_TOO_LARGE, Result.fail(ResultCode.PAYLOAD_TOO_LARGE));
                return;
            }
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            try {
                chain.doFilter(wrappedRequest, wrappedResponse);
            } finally {
                if (request.isAsyncStarted()) {
                    request.setAttribute(META_ATTRIBUTE, meta);
                    request.setAttribute(BODY_ATTRIBUTE, wrappedRequest.bodyAsString());
                } else {
                    try {
                        record(meta, wrappedRequest.bodyAsString(), wrappedResponse);
                    } catch (RuntimeException ex) {
                        log.warn("记录访问日志失败: {}", ex.getMessage());
                    }
                    wrappedResponse.copyBodyToResponse();
                }
            }
        } finally {
            RequestContext.clear();
            MDC.clear();
        }
    }

    private void finishAsync(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = response instanceof ContentCachingResponseWrapper
                ? (ContentCachingResponseWrapper) response
                : new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
            try {
                Object meta = request.getAttribute(META_ATTRIBUTE);
                Object body = request.getAttribute(BODY_ATTRIBUTE);
                if (meta instanceof RequestMeta) {
                    record((RequestMeta) meta, body == null ? "" : body.toString(), wrapper);
                }
            } catch (RuntimeException ex) {
                log.warn("记录异步访问日志失败: {}", ex.getMessage());
            }
        } finally {
            wrapper.copyBodyToResponse();
        }
    }

    private void record(RequestMeta meta, String rawBody, ContentCachingResponseWrapper response) {
        long cost = System.currentTimeMillis() - meta.getRequestTime();
        String path = meta.getPath();
        int maxBody = properties.getAccessLog().getMaxBodyLength();
        String requestBody = SensitiveTextMasker.truncate(SensitiveTextMasker.mask(rawBody), maxBody);
        String responseBody = SensitiveTextMasker.truncate(
                SensitiveTextMasker.mask(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)), 500);
        log.info("API {} {} ip={} status={} costMs={}", meta.getMethod(), path, meta.getClientIp(), response.getStatus(), cost);
        if (!properties.getAccessLog().isPersist() || excluded(path)) {
            return;
        }
        ApiRequestLog entity = new ApiRequestLog();
        entity.setTraceId(meta.getTraceId());
        entity.setAppId(trim(meta.getAppId(), 64));
        entity.setClientIp(trim(meta.getClientIp(), 64));
        entity.setHttpMethod(meta.getMethod());
        entity.setUri(trim(path, 255));
        entity.setStatusCode(response.getStatus());
        entity.setCostMs(cost);
        entity.setRequestBody(requestBody);
        entity.setResponseBody(responseBody);
        entity.setCreatedAt(LocalDateTime.now());
        apiLogService.save(entity);
    }

    private boolean excluded(String path) {
        return properties.getAccessLog().getExcludePaths().stream().anyMatch(path::equals);
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

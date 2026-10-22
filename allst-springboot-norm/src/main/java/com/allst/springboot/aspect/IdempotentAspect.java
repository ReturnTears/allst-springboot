package com.allst.springboot.aspect;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.header.HeaderNames;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.common.header.RequestMeta;
import com.allst.springboot.config.NormProperties;
import com.allst.springboot.domain.idem.IdempotentRecord;
import com.allst.springboot.domain.idem.IdempotentRepository;
import com.allst.springboot.domain.idem.IdempotentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 同一应用的幂等键只执行一次。成功后重放返回第一次的响应；失败删除记录，允许重试。
 * 对应规范：9 幂等设计。
 */
@Slf4j
@Aspect
@Component
@Order(20)
public class IdempotentAspect {

    private static final String KEY_PATTERN = "[A-Za-z0-9_-]{8,64}";

    private final IdempotentRepository repository;
    private final ObjectMapper objectMapper;
    private final NormProperties properties;

    public IdempotentAspect(IdempotentRepository repository, ObjectMapper objectMapper, NormProperties properties) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Around("@annotation(com.allst.springboot.annotation.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        RequestMeta meta = RequestContext.get();
        String idempotentKey = meta == null ? request.getHeader(HeaderNames.IDEMPOTENT_KEY) : meta.getIdempotentKey();
        if (idempotentKey == null || !idempotentKey.matches(KEY_PATTERN)) {
            throw new BizException(ResultCode.BAD_REQUEST, "幂等键需为 8 到 64 位字母、数字、下划线或中划线");
        }
        String appId = meta == null || meta.getAppId() == null ? "" : meta.getAppId();
        String api = request.getMethod() + " " + request.getRequestURI();
        String token = appId + ":" + idempotentKey;
        Optional<IdempotentRecord> existing = repository.findByIdemKey(token);
        if (existing.isPresent()) {
            return handleExisting(existing.get(), api, joinPoint);
        }
        IdempotentRecord record = new IdempotentRecord();
        record.setIdemKey(token);
        record.setAppId(appId);
        record.setApi(api);
        record.setStatus(IdempotentStatus.PROCESSING);
        record.setCreatedAt(LocalDateTime.now());
        try {
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            IdempotentRecord again = repository.findByIdemKey(token)
                    .orElseThrow(() -> new BizException(ResultCode.IDEMPOTENT_CONFLICT));
            return handleExisting(again, api, joinPoint);
        }
        try {
            Object result = joinPoint.proceed();
            record.setStatus(IdempotentStatus.SUCCESS);
            record.setHttpStatus(statusOf(result));
            record.setResponseJson(objectMapper.writeValueAsString(bodyOf(result)));
            record.setUpdatedAt(LocalDateTime.now());
            repository.save(record);
            return result;
        } catch (Throwable ex) {
            deleteQuietly(record);
            throw ex;
        }
    }

    private Object handleExisting(IdempotentRecord record, String api, ProceedingJoinPoint joinPoint) throws Exception {
        if (!api.equals(record.getApi())) {
            throw new BizException(ResultCode.IDEMPOTENT_CONFLICT, "幂等键已被其他接口使用");
        }
        if (record.getStatus() == IdempotentStatus.SUCCESS && record.getResponseJson() != null) {
            Result<?> body = objectMapper.readValue(record.getResponseJson(), Result.class);
            if (returnsResponseEntity(joinPoint)) {
                int status = record.getHttpStatus() == null ? 200 : record.getHttpStatus();
                return ResponseEntity.status(status).body(body);
            }
            return body;
        }
        if (record.getStatus() == IdempotentStatus.PROCESSING && expired(record)) {
            deleteQuietly(record);
            throw new BizException(ResultCode.IDEMPOTENT_PROCESSING, "上次请求处理超时，请使用同一幂等键重试");
        }
        throw new BizException(ResultCode.IDEMPOTENT_PROCESSING);
    }

    private boolean expired(IdempotentRecord record) {
        if (record.getCreatedAt() == null) {
            return false;
        }
        return record.getCreatedAt()
                .plusSeconds(properties.getIdempotent().getProcessingTimeoutSeconds())
                .isBefore(LocalDateTime.now());
    }

    private void deleteQuietly(IdempotentRecord record) {
        try {
            repository.delete(record);
        } catch (RuntimeException ex) {
            log.warn("清理幂等记录失败: {}", ex.getMessage());
        }
    }

    private boolean returnsResponseEntity(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return ResponseEntity.class.isAssignableFrom(signature.getReturnType());
    }

    private Object bodyOf(Object result) {
        if (result instanceof ResponseEntity) {
            return ((ResponseEntity<?>) result).getBody();
        }
        return result;
    }

    private int statusOf(Object result) {
        if (result instanceof ResponseEntity) {
            return ((ResponseEntity<?>) result).getStatusCode().value();
        }
        return 200;
    }
}

package com.allst.springboot.common.exception;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 控制器里抛出的异常统一成 JSON。过滤器里的 401/403 由安全入口自己写，不经过这里。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Void>> biz(BizException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiResult.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest().body(ApiResult.fail(ResultCode.VALIDATION, message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> denied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiResult.fail(ResultCode.FORBIDDEN, "没有权限"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResult<Void>> authentication(AuthenticationException ex) {
        if (ex instanceof LockedException) {
            return ResponseEntity.status(401).body(ApiResult.fail(ResultCode.LOCKED, "账号已锁定，请稍后再试"));
        }
        if (ex instanceof DisabledException) {
            return ResponseEntity.status(401).body(ApiResult.fail(ResultCode.DISABLED, "账号已停用"));
        }
        if (ex instanceof AccountExpiredException) {
            return ResponseEntity.status(401).body(ApiResult.fail(ResultCode.DISABLED, "账号已过期"));
        }
        return ResponseEntity.status(401).body(ApiResult.fail(ResultCode.BAD_CREDENTIALS, "用户名或密码错误"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Void>> conflict(DataIntegrityViolationException ex) {
        log.warn("数据约束冲突");
        return ResponseEntity.badRequest().body(ApiResult.fail(ResultCode.DUPLICATE, "数据冲突"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> unknown(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.status(500).body(ApiResult.fail(ResultCode.SERVER, "服务器内部错误"));
    }

    private String fieldMessage(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}

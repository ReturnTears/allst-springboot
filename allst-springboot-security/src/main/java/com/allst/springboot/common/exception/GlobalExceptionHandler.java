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
 * 控制器里抛出的异常统一成 JSON。
 * <p>
 * 过滤器里的 401/403 由 {@code RestAuthenticationEntryPoint} 和 {@code RestAccessDeniedHandler} 直接写响应，
 * 到不了这里。这里主要接住 {@code AuthController.login} 里 {@code AuthenticationManager.authenticate} 抛出的异常：
 * 不写这个方法的话，口令错误会变成 500，调用方无法区分「密码错了」和「服务器坏了」。
 * 用户不存在和口令错误都返回同一个文案，避免靠错误信息探测账号是否存在。
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

    /**
     * 锁定、停用、过期分开返回，方便前端提示。其余认证失败一律当成口令错误，不把内部异常类型透出去。
     */
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

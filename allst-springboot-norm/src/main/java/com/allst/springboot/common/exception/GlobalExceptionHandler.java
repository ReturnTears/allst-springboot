package com.allst.springboot.common.exception;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 把校验、业务、数据约束和未预期异常都收成 {@link Result}。响应体不包含堆栈。
 * 对应规范：7 统一封装异常。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex) {
        log.warn("业务异常 code={} message={}", ex.getResultCode().getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getResultCode().getHttpStatus())
                .body(Result.fail(ex.getResultCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleBody(MethodArgumentNotValidException ex) {
        return badRequest(join(ex.getBindingResult()));
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<Result<Void>> handleBind(org.springframework.validation.BindException ex) {
        return badRequest(join(ex));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(this::violationText)
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleJson(HttpMessageNotReadableException ex) {
        log.warn("JSON 解析失败: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.JSON_INVALID));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissing(MissingServletRequestParameterException ex) {
        return badRequest("缺少参数: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(ResultCode.METHOD_NOT_ALLOWED, "不支持的请求方法: " + ex.getMethod()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleData(DataIntegrityViolationException ex) {
        log.warn("数据约束冲突: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.fail(ResultCode.CONFLICT));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknown(Exception ex) {
        log.error("未处理异常", ex);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.VALIDATION_FAILED, message));
    }

    private String join(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private String violationText(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }
}

package com.allst.springboot.warehouse.api;

import com.allst.netty.client.SimulatorDisabledException;
import com.allst.netty.session.DeviceBusyException;
import com.allst.netty.session.DeviceOfflineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import jakarta.validation.ConstraintViolationException;

/**
 * 把接入层的业务异常收成统一的 JSON，避免看板只能看到一坨堆栈。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DeviceOfflineException.class)
    public ResponseEntity<ApiError> offline(DeviceOfflineException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("DEVICE_OFFLINE", "设备不在线: " + ex.deviceId()));
    }

    @ExceptionHandler(DeviceBusyException.class)
    public ResponseEntity<ApiError> busy(DeviceBusyException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiError.of("DEVICE_BUSY", "设备发送缓冲已满: " + ex.deviceId()));
    }

    @ExceptionHandler(SimulatorDisabledException.class)
    public ResponseEntity<ApiError> simulatorDisabled(SimulatorDisabledException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("SIMULATOR_DISABLED", "模拟扫码枪没有开启"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> validation(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> badJson(ServerWebInputException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", "请求体无法解析"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "请求没有完成" : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(ApiError.of("HTTP_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unknown(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL", "服务内部错误"));
    }
}

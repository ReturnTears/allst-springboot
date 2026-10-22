package com.allst.springboot.redis.config;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Void>> biz(BizException ex) {
        return ResponseEntity.badRequest().body(ApiResult.fail(400, ex.getMessage()));
    }

    @ExceptionHandler({ServletRequestBindingException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResult<Void>> missing(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResult.fail(400, "缺少必要的请求参数或登录凭证"));
    }

    @ExceptionHandler({RedisConnectionFailureException.class, RedisSystemException.class})
    public ResponseEntity<ApiResult<Void>> redis(RuntimeException ex) {
        if (isConnectionProblem(ex)) {
            log.warn("Redis 连接失败: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResult.fail(503, "Redis 未连接。请先启动 Redis，或查看 README 的 docker-compose 说明。"));
        }
        log.error("Redis 调用失败", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(500, "Redis 调用失败: " + rootMessage(ex)));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResult<Void>> database(DataAccessException ex) {
        log.error("数据库调用失败", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResult.fail(503, "数据库不可用。默认连接本机 MySQL allst_redis，无 MySQL 时可加 --spring.profiles.active=h2"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> other(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(500, rootMessage(ex)));
    }

    private boolean isConnectionProblem(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String name = current.getClass().getName();
            if (current instanceof RedisConnectionFailureException
                    || name.contains("RedisConnection")
                    || name.contains("ConnectException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}

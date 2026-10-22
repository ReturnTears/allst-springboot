package com.allst.springboot.log;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 访问日志。请求体已脱敏并截断。对应规范：8 请求日志。
 */
@Getter
@Setter
@Entity
@Table(name = "norm_api_log", indexes = {
        @Index(name = "idx_api_log_trace", columnList = "trace_id"),
        @Index(name = "idx_api_log_created", columnList = "created_at")
})
public class ApiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "app_id", length = 64)
    private String appId;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(nullable = false, length = 255)
    private String uri;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "cost_ms", nullable = false)
    private Long costMs;

    @Column(name = "request_body", length = 2000)
    private String requestBody;

    @Column(name = "response_body", length = 500)
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

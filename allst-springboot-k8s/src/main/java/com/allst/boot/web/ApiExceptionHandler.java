package com.allst.boot.web;

import com.allst.boot.k8s.K8sUnavailableException;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(K8sUnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(K8sUnavailableException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        HttpStatus httpStatus = ex.getStatus();
        String message = ex.getReason() == null ? ex.getMessage() : ex.getReason();
        return error(httpStatus, message, null);
    }

    @ExceptionHandler(KubernetesClientException.class)
    public ResponseEntity<Map<String, Object>> client(KubernetesClientException ex) {
        log.warn("Kubernetes API call failed: {}", ex.getMessage());
        int code = ex.getCode();
        HttpStatus status;
        if (code == 401) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (code == 403) {
            status = HttpStatus.FORBIDDEN;
        } else if (code == 404) {
            status = HttpStatus.NOT_FOUND;
        } else if (code == 409) {
            status = HttpStatus.CONFLICT;
        } else if (code >= 400 && code < 500) {
            status = HttpStatus.BAD_REQUEST;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }
        return error(status, ex.getMessage(), code == 0 ? null : code);
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, Integer kubernetesStatus) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (kubernetesStatus != null) {
            body.put("kubernetesStatus", kubernetesStatus);
        }
        return ResponseEntity.status(status).body(body);
    }
}

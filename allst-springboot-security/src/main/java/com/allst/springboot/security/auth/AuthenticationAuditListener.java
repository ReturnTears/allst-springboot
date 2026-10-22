package com.allst.springboot.security.auth;

import com.allst.springboot.common.web.ClientIpResolver;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.domain.entity.LoginAudit;
import com.allst.springboot.domain.repo.LoginAuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 认证成败写审计，并驱动锁定计数。监听器自己吞掉异常，避免审计故障把一次合法登录变成 500。
 */
@Slf4j
@Component
public class AuthenticationAuditListener {

    private final LoginAuditRepository audits;
    private final LoginAttemptService attempts;
    private final SecurityProperties properties;

    public AuthenticationAuditListener(LoginAuditRepository audits, LoginAttemptService attempts,
                                       SecurityProperties properties) {
        this.audits = audits;
        this.attempts = attempts;
        this.properties = properties;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        try {
            attempts.onSuccess(username);
            save(username, true, "登录成功");
        } catch (RuntimeException ex) {
            log.warn("记录登录成功失败: {}", ex.getClass().getSimpleName());
        }
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        if (!StringUtils.hasText(username)) {
            username = "-";
        }
        try {
            if (event.getException() instanceof BadCredentialsException) {
                attempts.onBadCredentials(username);
            }
            String message = event.getException() instanceof BadCredentialsException ? "用户名或密码错误" : "登录被拒绝";
            save(trim(username, 64), false, message);
        } catch (RuntimeException ex) {
            log.warn("记录登录失败失败: {}", ex.getClass().getSimpleName());
        }
    }

    private void save(String username, boolean success, String message) {
        LoginAudit audit = new LoginAudit();
        audit.setUsername(trim(username, 64));
        audit.setSuccess(success);
        audit.setMessage(trim(message, 128));
        audit.setCreatedAt(LocalDateTime.now());
        HttpServletRequest request = currentRequest();
        if (request != null) {
            audit.setIp(trim(ClientIpResolver.resolve(request, properties.getLogin().isTrustForwarded()), 64));
            audit.setUserAgent(trim(request.getHeader("User-Agent"), 256));
        }
        audits.save(audit);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

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
 * 监听认证框架发出的事件，而不是在每个登录接口里各写一遍审计。
 * 用户名口令登录、会话登录、HTTP Basic 都会经过 {@code AuthenticationManager}，所以都会到这里。
 * <p>
 * 监听器自己吞掉异常：审计库出问题不应该把一次已经成功的登录变成 500。
 * 只处理口令错误的失败事件来累计锁定；账号已锁定时不再加次数。
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

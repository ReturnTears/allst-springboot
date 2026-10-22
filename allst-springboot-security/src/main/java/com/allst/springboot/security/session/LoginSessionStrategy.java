package com.allst.springboot.security.session;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录成功后先换会话号，再登记到 SessionRegistry。自定义登录过滤器不会自动套用 HttpSecurity 里的那份策略。
 * 见 docs/04-session-management.md。
 */
public class LoginSessionStrategy implements SessionAuthenticationStrategy {

    private final ConcurrentSessionControlAuthenticationStrategy concurrent;
    private final RegisterSessionAuthenticationStrategy register;

    public LoginSessionStrategy(SessionRegistry sessionRegistry, int maximumSessions) {
        this.concurrent = new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry);
        this.concurrent.setMaximumSessions(maximumSessions);
        this.concurrent.setExceptionIfMaximumExceeded(false);
        this.register = new RegisterSessionAuthenticationStrategy(sessionRegistry);
    }

    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) == null) {
            request.getSession(true);
        }
        concurrent.onAuthentication(authentication, request, response);
        request.changeSessionId();
        register.onAuthentication(authentication, request, response);
    }
}

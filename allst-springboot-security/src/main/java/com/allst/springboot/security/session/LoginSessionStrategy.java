package com.allst.springboot.security.session;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录成功后的会话处理。自定义的表单过滤器不会自动使用 HttpSecurity 里配置的那份策略，所以单独写在这里。
 * 顺序不能反：先处理旧会话（超过上限就让最早的失效），再更换会话号，最后把新会话号登记进 SessionRegistry。
 * 换号要放在登记之前，否则注册表里留下的是已经作废的旧会话号。
 */
public class LoginSessionStrategy implements SessionAuthenticationStrategy {

    private final ConcurrentSessionControlAuthenticationStrategy concurrent;
    private final RegisterSessionAuthenticationStrategy register;

    public LoginSessionStrategy(SessionRegistry sessionRegistry, int maximumSessions) {
        this.concurrent = new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry);
        this.concurrent.setMaximumSessions(maximumSessions);
        // false：超限时踢掉最早的会话，让这次登录成功。true 的话新登录会被拒绝，旧会话继续占用名额。
        this.concurrent.setExceptionIfMaximumExceeded(false);
        this.register = new RegisterSessionAuthenticationStrategy(sessionRegistry);
    }

    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) == null) {
            // changeSessionId 要求会话已经存在。表单过滤器不会事先建会话，这里补上。
            request.getSession(true);
        }
        concurrent.onAuthentication(authentication, request, response);
        request.changeSessionId();
        register.onAuthentication(authentication, request, response);
    }
}

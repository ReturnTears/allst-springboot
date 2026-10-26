package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.web.dto.SessionView;
import com.allst.springboot.web.dto.UserView;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话查询。登录和退出不经过这个类，它们由过滤链里的表单过滤器和退出过滤器处理。
 * <p>
 * {@code GET /session/csrf} 放行，用来把 CSRF 令牌发给前端。
 * {@code GET /session/me} 要求已经有会话。
 * {@code GET /session/active} 在链上要求 ROLE_ADMIN，只返回会话号前 8 位，避免把完整会话号交给浏览器。
 */
@RestController
@RequestMapping("/session")
public class SessionController {

    private final SessionRegistry sessionRegistry;

    public SessionController(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping("/csrf")
    public ApiResult<Map<String, String>> csrf(CsrfToken csrfToken) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("headerName", csrfToken.getHeaderName());
        body.put("parameterName", csrfToken.getParameterName());
        body.put("token", csrfToken.getToken());
        return ApiResult.ok(body);
    }

    @GetMapping("/me")
    public ApiResult<UserView> me(Authentication authentication) {
        return ApiResult.ok(UserView.from((SecurityUser) authentication.getPrincipal()));
    }

    @GetMapping("/active")
    public ApiResult<List<SessionView>> active() {
        List<SessionView> views = new ArrayList<>();
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!(principal instanceof SecurityUser)) {
                continue;
            }
            SecurityUser user = (SecurityUser) principal;
            for (SessionInformation information : sessionRegistry.getAllSessions(principal, false)) {
                String sessionId = information.getSessionId();
                String prefix = sessionId.length() <= 8 ? sessionId : sessionId.substring(0, 8);
                Date lastRequest = information.getLastRequest();
                LocalDateTime last = lastRequest == null
                        ? null
                        : LocalDateTime.ofInstant(lastRequest.toInstant(), ZoneId.systemDefault());
                views.add(new SessionView(prefix, user.getUsername(), last));
            }
        }
        return ApiResult.ok(views);
    }
}

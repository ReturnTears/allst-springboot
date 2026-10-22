package com.allst.springboot.security.extension;

import com.allst.springboot.security.auth.LoginGuard;
import com.allst.springboot.security.jwt.TokenCustomizer;
import com.allst.springboot.security.protocol.SecurityProtocol;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时能看到哪些协议和扩展点已经被装进来。见 docs/08-extensible-architecture.md。
 */
@Component
public class SecurityCatalog {

    private final List<SecurityProtocol> protocols;
    private final List<LoginGuard> guards;
    private final List<TokenCustomizer> customizers;

    public SecurityCatalog(List<SecurityProtocol> protocols, List<LoginGuard> guards, List<TokenCustomizer> customizers) {
        this.protocols = protocols;
        this.guards = guards;
        this.customizers = customizers;
    }

    public Map<String, Object> describe() {
        List<Map<String, Object>> protocolViews = new ArrayList<>();
        for (SecurityProtocol protocol : protocols) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", protocol.id());
            view.put("title", protocol.title());
            view.put("enabled", protocol.enabled());
            view.put("description", protocol.description());
            protocolViews.add(view);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("protocols", protocolViews);
        body.put("loginGuards", simpleNames(guards));
        body.put("tokenCustomizers", simpleNames(customizers));
        body.put("filterChains", List.of("oauth2-login（条件）", "api-jwt", "http-basic", "session-cookie", "public"));
        body.put("methodSecurity", "PreAuthorize / PostAuthorize / PreFilter / Secured / RolesAllowed");
        return body;
    }

    private List<String> simpleNames(List<?> beans) {
        List<String> names = new ArrayList<>();
        for (Object bean : beans) {
            names.add(bean.getClass().getSimpleName());
        }
        return names;
    }
}

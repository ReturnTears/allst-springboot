package com.allst.springboot.security.method;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Service;

import javax.annotation.security.RolesAllowed;
import java.util.List;

/**
 * 方法注解示例。必须通过 Spring 代理调用，控制器里不要把逻辑写回这个类内部自调用。
 */
@Service
public class MethodDemoService {

    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "admin";
    }

    @PreAuthorize("hasAuthority('user:read')")
    public String readUsers() {
        return "user:read";
    }

    @PreAuthorize("hasPermission(#userId, 'user', 'read')")
    public String readUser(Long userId) {
        return "user:" + userId;
    }

    @PostAuthorize("returnObject == authentication.name or hasRole('ADMIN')")
    public String ownerName(String username) {
        return username;
    }

    @PreFilter("filterObject == authentication.name or hasRole('ADMIN')")
    public List<String> filterNames(List<String> names) {
        return names;
    }

    @Secured("ROLE_AUDITOR")
    public String securedAuditor() {
        return "auditor";
    }

    @RolesAllowed("ROLE_USER")
    public String jsr250User() {
        return "user";
    }
}

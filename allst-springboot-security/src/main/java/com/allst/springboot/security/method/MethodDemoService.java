package com.allst.springboot.security.method;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Service;

import javax.annotation.security.RolesAllowed;
import java.util.List;

/**
 * 方法注解示例。控制器只是转一手，判断发生在这些方法进入之前或返回之后。
 * <p>
 * {@code GET /api/method/admin} → {@link #adminOnly()}，表达式 hasRole('ADMIN')
 * {@code GET /api/method/read} → {@link #readUsers()}，要有 user:read
 * {@code GET /api/method/user/{id}} → {@link #readUser(Long)}，走 PermissionEvaluator
 * {@code GET /api/method/owner} → {@link #ownerName(String)}，返回后再看是不是本人或管理员
 * {@code POST /api/method/filter} → {@link #filterNames(List)}，先把参数列表过滤掉别人的名字
 * {@code GET /api/method/secured-auditor} → {@link #securedAuditor()}，只要 ROLE_AUDITOR，管理员并不自动拥有
 * {@code GET /api/method/jsr250-user} → {@link #jsr250User()}，只要 ROLE_USER，管理员并不自动拥有
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

    /** 方法执行前判断。#userId 就是参数。资源类型不是 user 时，计算器直接拒绝。 */
    @PreAuthorize("hasPermission(#userId, 'user', 'read')")
    public String readUser(Long userId) {
        return "user:" + userId;
    }

    /** 方法已经算出返回值之后再判断。returnObject 就是这个用户名。 */
    @PostAuthorize("returnObject == authentication.name or hasRole('ADMIN')")
    public String ownerName(String username) {
        return username;
    }

    /** 进入方法前改参数。filterObject 是列表里的每一项，对不上当前用户、又不是管理员的会被拿掉。 */
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

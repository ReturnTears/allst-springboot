package com.allst.springboot.bootstrap;

import com.allst.springboot.domain.entity.SecPermission;
import com.allst.springboot.domain.entity.SecRole;
import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.domain.repo.SecPermissionRepository;
import com.allst.springboot.domain.repo.SecRoleRepository;
import com.allst.springboot.domain.repo.SecUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 空库时写入三个示例账号。生产环境不要依赖这段逻辑，它会在日志里打出初始口令。
 * 角色和权限先保存，再挂到用户上，是因为多对多关系需要两边都已经有主键。
 */
@Slf4j
@Component
public class SecurityDataInitializer implements ApplicationRunner {

    private final SecUserRepository users;
    private final SecRoleRepository roles;
    private final SecPermissionRepository permissions;
    private final PasswordEncoder passwordEncoder;

    public SecurityDataInitializer(SecUserRepository users, SecRoleRepository roles,
                                   SecPermissionRepository permissions, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        SecPermission userRead = permission("user:read", "查看用户");
        SecPermission userWrite = permission("user:write", "维护用户");
        SecPermission profileWrite = permission("profile:write", "修改本人资料");
        SecPermission auditRead = permission("audit:read", "查看登录审计");
        SecPermission sessionRead = permission("session:read", "查看在线会话");

        SecRole admin = role("ROLE_ADMIN", "管理员", userRead, userWrite, profileWrite, auditRead, sessionRead);
        SecRole user = role("ROLE_USER", "普通用户", userRead, profileWrite);
        SecRole auditor = role("ROLE_AUDITOR", "审计员", userRead, auditRead);

        account("admin", "Manage@123", "管理员", admin);
        account("alice", "Reader@123", "爱丽丝", user);
        account("auditor", "Audit@123", "审计员", auditor);
        log.info("示例账号已就绪：admin / Manage@123，alice / Reader@123，auditor / Audit@123");
    }

    private SecPermission permission(String code, String name) {
        SecPermission permission = new SecPermission();
        permission.setCode(code);
        permission.setName(name);
        return permissions.save(permission);
    }

    private SecRole role(String code, String name, SecPermission... grants) {
        SecRole role = new SecRole();
        role.setCode(code);
        role.setName(name);
        role.setPermissions(new HashSet<>(java.util.Arrays.asList(grants)));
        return roles.save(role);
    }

    private void account(String username, String rawPassword, String displayName, SecRole role) {
        SecUser user = new SecUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        Set<SecRole> assigned = new HashSet<>();
        assigned.add(role);
        user.setRoles(assigned);
        users.save(user);
    }
}

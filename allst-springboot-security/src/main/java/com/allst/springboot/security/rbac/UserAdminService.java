package com.allst.springboot.security.rbac;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.domain.entity.SecRole;
import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.domain.repo.SecRoleRepository;
import com.allst.springboot.domain.repo.SecUserRepository;
import com.allst.springboot.security.password.PasswordPolicy;
import com.allst.springboot.security.password.PasswordService;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.web.dto.UserCreateRequest;
import com.allst.springboot.web.dto.UserView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户管理。调用链以 {@code GET /api/users} 为例：
 * <pre>
 * JwtAuthenticationFilter     确认令牌，把权限放进上下文
 *   → apiChain                要求 user:read，没有就 403，到不了控制器
 *   → UserController.list
 *   → UserAdminService.list   方法上再写一次 @PreAuthorize
 * </pre>
 * URL 和方法各写一次，是因为以后如果有别的服务在进程内调用 list，URL 规则拦不住它。
 */
@Service
public class UserAdminService {

    private final SecUserRepository users;
    private final SecRoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final PasswordService passwordService;
    private final SecurityProperties properties;

    public UserAdminService(SecUserRepository users, SecRoleRepository roles, PasswordEncoder passwordEncoder,
                            PasswordService passwordService, SecurityProperties properties) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.passwordService = passwordService;
        this.properties = properties;
    }

    @PreAuthorize("hasAuthority('user:read')")
    @Transactional(readOnly = true)
    public List<UserView> list() {
        List<UserView> views = new ArrayList<>();
        for (SecUser user : users.findAll()) {
            views.add(UserView.from(user, passwordService.isExpired(user)));
        }
        return views;
    }

    @PreAuthorize("hasAuthority('user:write')")
    @Transactional
    public UserView create(UserCreateRequest request) {
        PasswordPolicy.checkDisplayName(request.getDisplayName());
        PasswordPolicy.check(request.getUsername(), request.getPassword(), properties.getPassword().getDenylist());
        if (users.existsByUsername(request.getUsername())) {
            throw new BizException(ResultCode.DUPLICATE, 400, "用户名已存在");
        }
        SecUser user = new SecUser();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setRoles(resolveRoles(request.getRoleCodes()));
        return UserView.from(users.save(user), false);
    }

    @PreAuthorize("hasAuthority('user:write')")
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        passwordService.reset(userId, newPassword);
    }

    @PreAuthorize("hasAuthority('user:write')")
    @Transactional
    public UserView setEnabled(Long userId, boolean enabled) {
        SecurityUser current = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (current.getId().equals(userId) && !enabled) {
            throw new BizException(ResultCode.VALIDATION, 400, "不能停用当前登录账号");
        }
        SecUser user = users.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.VALIDATION, 400, "用户不存在"));
        user.setEnabled(enabled);
        return UserView.from(users.save(user), passwordService.isExpired(user));
    }

    private Set<SecRole> resolveRoles(List<String> roleCodes) {
        List<String> codes = roleCodes == null || roleCodes.isEmpty() ? List.of("ROLE_USER") : roleCodes;
        Set<SecRole> resolved = new HashSet<>();
        for (String code : codes) {
            resolved.add(roles.findByCode(code)
                    .orElseThrow(() -> new BizException(ResultCode.VALIDATION, 400, "角色不存在: " + code)));
        }
        return resolved;
    }
}

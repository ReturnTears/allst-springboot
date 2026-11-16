package com.allst.springboot.security.sms;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.domain.entity.SecRole;
import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.domain.repo.SecRoleRepository;
import com.allst.springboot.domain.repo.SecUserRepository;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.security.user.SecurityUserDetailsService;
import com.allst.springboot.web.dto.TokenView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 短信登录（路线 B：控制器编排，不挂全局 AuthenticationManager，与 LDAP 一致）。
 * <p>
 * 调用链：校验验证码 → 按手机号查本地账号，没有就自动注册（默认 ROLE_USER）→
 * 检查账号状态 → 用 SecurityUserDetailsService 加载（展开角色权限）→ JwtTokenService.issue。
 * <p>
 * 自动注册的账号用户名即手机号，密码为随机不可登录值（短信登录不靠密码）。
 * 失败时由控制器补发认证失败事件，复用审计与锁定。
 */
@Service
public class SmsLoginService {

    private final SmsCodeService codes;
    private final SecUserRepository users;
    private final SecRoleRepository roles;
    private final SecurityUserDetailsService userDetailsService;
    private final JwtTokenService tokens;
    private final PasswordEncoder passwordEncoder;

    public SmsLoginService(SmsCodeService codes, SecUserRepository users, SecRoleRepository roles,
                           SecurityUserDetailsService userDetailsService, JwtTokenService tokens,
                           PasswordEncoder passwordEncoder) {
        this.codes = codes;
        this.users = users;
        this.roles = roles;
        this.userDetailsService = userDetailsService;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenView login(String phone, String code) {
        if (!codes.verify(phone, code)) {
            throw new BizException(ResultCode.SMS_CODE_INVALID, 401, "验证码错误");
        }
        SecUser user = users.findByPhone(phone).orElseGet(() -> register(phone));
        if (!user.isEnabled() || !user.isAccountNonLocked() || !user.isAccountNonExpired()) {
            throw new BizException(ResultCode.DISABLED, 401, "账号不可用");
        }
        SecurityUser principal = (SecurityUser) userDetailsService.loadUserByUsername(user.getUsername());
        return tokens.issue(principal);
    }

    /** 未注册手机号自动注册。用户名即手机号，挂默认 ROLE_USER，密码为随机值（不可用于密码登录）。 */
    private SecUser register(String phone) {
        SecUser user = new SecUser();
        user.setUsername(phone);
        user.setPhone(phone);
        user.setDisplayName(phone);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        Set<SecRole> assigned = new HashSet<>();
        roles.findByCode("ROLE_USER").ifPresent(assigned::add);
        user.setRoles(assigned);
        return users.save(user);
    }
}
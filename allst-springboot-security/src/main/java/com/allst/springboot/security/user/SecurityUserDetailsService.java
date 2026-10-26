package com.allst.springboot.security.user;

import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.domain.repo.SecUserRepository;
import com.allst.springboot.security.password.PasswordService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证时按用户名加载账号。方法上有事务，是因为角色和权限是懒加载的，
 * 必须在这次查询还没结束时把它们拷贝进 {@link SecurityUser}，事务外再碰集合会报错。
 * <p>
 * 锁定时间到了就在这里解开，而不是另写定时任务：用户下次登录自然会走到这个方法。
 */
@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final SecUserRepository users;
    private final PasswordService passwordService;

    public SecurityUserDetailsService(SecUserRepository users, PasswordService passwordService) {
        this.users = users;
        this.passwordService = passwordService;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SecUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        unlockIfDue(user);
        return new SecurityUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getDisplayName(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                passwordService.isExpired(user),
                SecurityUsers.authorities(user));
    }

    private void unlockIfDue(SecUser user) {
        if (user.isAccountNonLocked() || user.getLockUntil() == null) {
            return;
        }
        if (!user.getLockUntil().isAfter(LocalDateTime.now())) {
            user.setAccountNonLocked(true);
            user.setFailedAttempts(0);
            user.setLockUntil(null);
            users.save(user);
        }
    }
}

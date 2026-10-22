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
 * 认证时按用户名加载账号，并在锁定时间已过时自动解锁。见 docs/01-authentication.md。
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

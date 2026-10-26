package com.allst.springboot.security.method;

import com.allst.springboot.security.user.SecurityUser;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * {@code hasPermission} 的判断。URL 上的 hasAuthority 不走这里。
 * 本人访问自己的用户编号时放行，这样没有 user:read 的人仍能看自己的资料。
 * 未知资源类型一律拒绝，避免新写的类型因为没人处理而被默认允许。
 */
@Component
public class DomainPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return hasAuthority(authentication, String.valueOf(permission));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (!"user".equals(targetType) || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (isSelf(authentication, targetId)) {
            return true;
        }
        return hasAuthority(authentication, targetType + ":" + permission);
    }

    private boolean isSelf(Authentication authentication, Serializable targetId) {
        if (!(authentication.getPrincipal() instanceof SecurityUser) || targetId == null) {
            return false;
        }
        return String.valueOf(((SecurityUser) authentication.getPrincipal()).getId()).equals(String.valueOf(targetId));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (granted.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }
}

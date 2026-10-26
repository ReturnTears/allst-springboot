package com.allst.springboot.security.user;

import com.allst.springboot.domain.entity.SecPermission;
import com.allst.springboot.domain.entity.SecRole;
import com.allst.springboot.domain.entity.SecUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 把「用户 → 角色 → 权限」摊平成一串 {@code GrantedAuthority}。
 * {@code hasRole('ADMIN')} 认的是 {@code ROLE_ADMIN}，{@code hasAuthority('user:read')} 认的是权限码，两边都放进同一个列表。
 * 必须在事务里调用：角色和权限是懒加载的，事务结束后再遍历会抛 LazyInitializationException。
 */
public final class SecurityUsers {

    private SecurityUsers() {
    }

    public static List<SimpleGrantedAuthority> authorities(SecUser user) {
        Set<String> codes = new LinkedHashSet<>();
        for (SecRole role : user.getRoles()) {
            codes.add(role.getCode());
            for (SecPermission permission : role.getPermissions()) {
                codes.add(permission.getCode());
            }
        }
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String code : codes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }
        return authorities;
    }
}

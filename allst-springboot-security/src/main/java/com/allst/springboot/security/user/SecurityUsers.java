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
 * 把角色码和权限码展开成 GrantedAuthority。必须在事务里调用，这样懒加载的集合才读得到。
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

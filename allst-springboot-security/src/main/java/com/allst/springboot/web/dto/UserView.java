package com.allst.springboot.web.dto;

import com.allst.springboot.domain.entity.SecPermission;
import com.allst.springboot.domain.entity.SecRole;
import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.security.user.SecurityUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserView {

    private final Long id;
    private final String username;
    private final String displayName;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean passwordExpired;
    private final List<String> roles;
    private final List<String> permissions;

    public static UserView from(SecurityUser user) {
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (authority.getAuthority().startsWith("ROLE_")) {
                roles.add(authority.getAuthority());
            } else {
                permissions.add(authority.getAuthority());
            }
        }
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.isEnabled(),
                user.isAccountNonLocked(), user.isPasswordExpired(), roles, permissions);
    }

    public static UserView from(SecUser user, boolean passwordExpired) {
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        for (SecRole role : user.getRoles()) {
            roles.add(role.getCode());
            for (SecPermission permission : role.getPermissions()) {
                if (!permissions.contains(permission.getCode())) {
                    permissions.add(permission.getCode());
                }
            }
        }
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.isEnabled(),
                user.isAccountNonLocked(), passwordExpired, roles, permissions);
    }
}

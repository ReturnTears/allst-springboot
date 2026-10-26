package com.allst.springboot.security.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

/**
 * 放进 SecurityContext 的当前用户。权限在构造时已经从角色展开成字符串，后面不再碰数据库实体。
 * <p>
 * {@code equals}/{@code hashCode} 只看用户名。会话注册表用它们判断「这两次登录是不是同一个人」。
 * {@code isCredentialsNonExpired} 固定返回 true：密码过期仍允许登录，改密限制交给 {@code PasswordExpiryFilter}。
 * 如果这里返回 false，用户会直接登录失败，连改密接口都进不去。
 */
@Getter
public class SecurityUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String displayName;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean accountNonExpired;
    private final boolean passwordExpired;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUser(Long id, String username, String password, String displayName, boolean enabled,
                        boolean accountNonLocked, boolean accountNonExpired, boolean passwordExpired,
                        Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.accountNonExpired = accountNonExpired;
        this.passwordExpired = passwordExpired;
        this.authorities = authorities;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 密码过期仍允许登录，由 PasswordExpiryFilter 限制除改密以外的接口。
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SecurityUser)) {
            return false;
        }
        return username.equals(((SecurityUser) other).username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}

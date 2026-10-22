package com.allst.springboot.security.protocol;

import com.allst.springboot.config.SecurityProperties;
import org.springframework.stereotype.Component;

@Component
public class LdapBindProtocol implements SecurityProtocol {

    private final SecurityProperties properties;

    public LdapBindProtocol(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String id() {
        return "ldap-bind";
    }

    @Override
    public String title() {
        return "LDAP 绑定";
    }

    @Override
    public boolean enabled() {
        return properties.getLdap().isEnabled();
    }

    @Override
    public String description() {
        return "POST /api/auth/ldap。绑定成功后仍按本地角色授权。默认关闭，profile ldap 打开内嵌目录。";
    }
}

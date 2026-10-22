package com.allst.springboot.security.auth;

import com.allst.springboot.config.SecurityProperties;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * security.maintenance=true 时拒绝所有登录入口。
 */
@Component
public class MaintenanceLoginGuard implements LoginGuard {

    private final SecurityProperties properties;

    public MaintenanceLoginGuard(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> reject(HttpServletRequest request) {
        if (properties.isMaintenance()) {
            return Optional.of("系统维护中，暂停登录");
        }
        return Optional.empty();
    }
}

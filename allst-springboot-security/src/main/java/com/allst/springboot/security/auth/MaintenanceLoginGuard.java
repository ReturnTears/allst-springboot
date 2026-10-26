package com.allst.springboot.security.auth;

import com.allst.springboot.config.SecurityProperties;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * {@code security.maintenance=true} 时拒绝全部登录入口。已经持有的访问令牌不受影响，只是不能再登录或刷新。
 * 放在限流前面：维护期间不必再消耗计数。
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

package com.allst.springboot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关的可调参数，对应 application.yml 里的 security 节点。
 * 放在配置文件而不是写死在代码里，是为了换环境时只改配置：密钥、锁定次数、密码有效期、要不要开 OAuth2。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /** 为 true 时登录入口直接拒绝，用于维护窗口。 */
    private boolean maintenance;

    private final Jwt jwt = new Jwt();
    private final Login login = new Login();
    private final Password password = new Password();
    private final Session session = new Session();
    private final OAuth2 oauth2 = new OAuth2();
    private final Ldap ldap = new Ldap();

    @Getter
    @Setter
    public static class Jwt {
        private String issuer;
        private String secret;
        private int accessTokenMinutes = 30;
        private int refreshTokenDays = 7;
    }

    @Getter
    @Setter
    public static class Login {
        private int maxFailures = 5;
        private int lockMinutes = 15;
        private int ipLimitPerMinute = 20;
        private boolean trustForwarded;
    }

    @Getter
    @Setter
    public static class Password {
        private int historySize = 5;
        private int maxAgeDays = 90;
        private List<String> denylist = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Session {
        private int maximum = 1;
    }

    @Getter
    @Setter
    public static class OAuth2 {
        private boolean enabled;
    }

    @Getter
    @Setter
    public static class Ldap {
        private boolean enabled;
    }
}

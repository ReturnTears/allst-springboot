package com.allst.springboot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全示例的可调参数，前缀 {@code security}。各项含义见 docs 目录对应文章。
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

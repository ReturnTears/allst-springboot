package com.allst.springboot.config;

import com.allst.springboot.security.method.DomainPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

/**
 * 方法级安全。URL 规则只能看到「这个地址要不要登录」，一个地址里的不同动作要用注解再分一次。
 * <p>
 * {@code proxyTargetClass = true}：这些服务类没有接口。默认的 JDK 代理要求有接口，不开这个开关，注解不会生效。
 * 注解必须写在 public 方法上，并且从别的 Bean 调用。同一个类内部自己调用，不会经过代理，注解等于没写。
 * <p>
 * 角色继承只对 {@code hasRole} 这种表达式生效。{@code @Secured} 和 {@code @RolesAllowed} 要的是精确的角色名，
 * 所以管理员调用 {@code /api/method/jsr250-user} 会 403。这是演示，业务代码要管理员也能进时请用表达式。
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true, proxyTargetClass = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

    private final DomainPermissionEvaluator permissionEvaluator;

    public MethodSecurityConfig(DomainPermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * ADMIN 同时具备 AUDITOR 和 USER 的角色。这只影响表达式，不影响 {@code @Secured}。
     * 权限码（user:read 这种）不在继承里，管理员能做的事是初始化时写进角色-权限表的。
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_AUDITOR\nROLE_ADMIN > ROLE_USER");
        return hierarchy;
    }

    /**
     * URL 规则（hasRole、hasAuthority）用这一份。和下面的方法表达式各配一次，否则只有注解认角色继承，过滤链不认。
     */
    @Bean
    public DefaultWebSecurityExpressionHandler webExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    /** {@code @PreAuthorize} 用这一份。不覆盖的话，方法注解看不到上面的角色继承，也走不到自定义的 PermissionEvaluator。 */
    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy());
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}

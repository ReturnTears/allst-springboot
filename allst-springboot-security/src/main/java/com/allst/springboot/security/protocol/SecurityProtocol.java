package com.allst.springboot.security.protocol;

/**
 * 一种登录方式的说明，只给目录接口展示。实现并注册成 Bean 后，{@code GET /api/security/modules} 会多一项。
 * 这不会自动打开入口：还要在配置里增加匹配该路径的 {@code SecurityFilterChain}。
 */
public interface SecurityProtocol {

    String id();

    String title();

    boolean enabled();

    String description();
}

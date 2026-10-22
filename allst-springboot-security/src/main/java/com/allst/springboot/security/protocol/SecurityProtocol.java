package com.allst.springboot.security.protocol;

/**
 * 一种登录协议。新增协议时实现本接口并注册成 Bean，目录接口会把它列出来。
 * 真正的过滤链仍要单独加一条 SecurityFilterChain。见 docs/06-protocol-integration.md、docs/08-extensible-architecture.md。
 */
public interface SecurityProtocol {

    String id();

    String title();

    boolean enabled();

    String description();
}

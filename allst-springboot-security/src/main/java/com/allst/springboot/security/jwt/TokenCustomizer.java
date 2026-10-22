package com.allst.springboot.security.jwt;

import com.allst.springboot.security.user.SecurityUser;

import java.util.Map;

/**
 * 签发访问令牌前的扩展点。新增声明时实现本接口并注册成 Bean，不要改 JwtTokenService。
 * 见 docs/08-extensible-architecture.md。
 */
public interface TokenCustomizer {

    void customize(Map<String, Object> claims, SecurityUser user);
}

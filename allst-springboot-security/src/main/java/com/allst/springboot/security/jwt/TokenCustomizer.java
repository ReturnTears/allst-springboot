package com.allst.springboot.security.jwt;

import com.allst.springboot.security.user.SecurityUser;

import java.util.Map;

/**
 * 签发访问令牌前的扩展点。{@code JwtTokenService.issue} 会找到所有实现，在签名之前调用。
 * 要往令牌里加字段时实现本接口并注册成 Bean，不必改签发方法。声明只是提示，授权仍以数据库为准。
 */
public interface TokenCustomizer {

    void customize(Map<String, Object> claims, SecurityUser user);
}

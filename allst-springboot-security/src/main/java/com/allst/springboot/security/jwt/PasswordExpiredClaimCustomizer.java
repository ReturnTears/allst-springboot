package com.allst.springboot.security.jwt;

import com.allst.springboot.security.user.SecurityUser;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 把 {@code passwordExpired} 写进访问令牌，登录页可以直接提示去改密。
 * 过滤器拦截时仍读数据库里的 {@code SecurityUser}，改了这个声明并不能绕过 {@code PasswordExpiryFilter}。
 */
@Component
public class PasswordExpiredClaimCustomizer implements TokenCustomizer {

    @Override
    public void customize(Map<String, Object> claims, SecurityUser user) {
        claims.put("passwordExpired", user.isPasswordExpired());
    }
}

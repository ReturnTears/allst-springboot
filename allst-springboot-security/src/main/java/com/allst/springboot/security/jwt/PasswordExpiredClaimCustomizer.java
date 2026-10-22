package com.allst.springboot.security.jwt;

import com.allst.springboot.security.user.SecurityUser;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 把密码是否过期放进访问令牌，前端不用再猜。过期与否仍以数据库为准，这个声明只是提示。
 */
@Component
public class PasswordExpiredClaimCustomizer implements TokenCustomizer {

    @Override
    public void customize(Map<String, Object> claims, SecurityUser user) {
        claims.put("passwordExpired", user.isPasswordExpired());
    }
}

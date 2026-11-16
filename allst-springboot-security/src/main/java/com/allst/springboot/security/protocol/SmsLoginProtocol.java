package com.allst.springboot.security.protocol;

import com.allst.springboot.config.SecurityProperties;
import org.springframework.stereotype.Component;

/** 目录项：短信验证码登录。开关跟着 {@code security.sms.enabled}，入口在 SmsLoginController。 */
@Component
public class SmsLoginProtocol implements SecurityProtocol {

    private final SecurityProperties properties;

    public SmsLoginProtocol(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String id() {
        return "sms-login";
    }

    @Override
    public String title() {
        return "短信验证码登录";
    }

    @Override
    public boolean enabled() {
        return properties.getSms().isEnabled();
    }

    @Override
    public String description() {
        return "POST /api/auth/sms/send 发码，POST /api/auth/sms/login 验证码登录，未注册手机号自动注册。";
    }
}
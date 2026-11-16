package com.allst.springboot.security.protocol;

import com.allst.springboot.config.SecurityProperties;
import org.springframework.stereotype.Component;

/** 目录项：扫码登录。开关跟着 {@code security.qr.enabled}，入口在 QrLoginController。 */
@Component
public class QrLoginProtocol implements SecurityProtocol {

    private final SecurityProperties properties;

    public QrLoginProtocol(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String id() {
        return "qr-login";
    }

    @Override
    public String title() {
        return "扫码登录";
    }

    @Override
    public boolean enabled() {
        return properties.getQr().isEnabled();
    }

    @Override
    public String description() {
        return "GET /api/auth/qr/create 建码，手机 POST /api/auth/qr/scan、/confirm，PC 轮询 /status 取 JWT。";
    }
}
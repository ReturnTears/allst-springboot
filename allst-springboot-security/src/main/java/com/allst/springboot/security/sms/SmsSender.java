package com.allst.springboot.security.sms;

/**
 * 短信发送扩展点。演示用 {@link LogSmsSender}（仅打印），生产用 {@link AliyunSmsSender}。
 * 由 {@code security.sms.provider} 决定装配哪一个，换通道不改登录逻辑。
 */
public interface SmsSender {

    /**
     * 发送验证码短信。
     *
     * @param phone 手机号
     * @param code  验证码
     */
    void send(String phone, String code);
}
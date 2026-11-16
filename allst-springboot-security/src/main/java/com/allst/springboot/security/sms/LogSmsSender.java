package com.allst.springboot.security.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 演示用短信发送：只把验证码打进日志，不调用任何外部通道。
 * {@code security.sms.provider=log}（默认）时装配。生产请切到 aliyun。
 * <p>
 * 注意：验证码进日志仅限本机演示，生产环境严禁打印验证码。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "security.sms", name = "provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSender implements SmsSender {

    @Override
    public void send(String phone, String code) {
        log.info("[演示短信] 手机号={} 验证码={}（仅本机演示，生产请切换 aliyun 并勿打印验证码）", phone, code);
    }
}
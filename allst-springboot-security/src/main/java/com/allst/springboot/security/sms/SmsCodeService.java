package com.allst.springboot.security.sms;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码状态机。单实例内存存储；多实例需换 Redis 等共享存储，否则 A 实例发的码 B 实例验不了。
 * <p>
 * 防护点：
 * <ul>
 *   <li>发送间隔限流（默认 60 秒），防短信轰炸；</li>
 *   <li>试错上限（默认 5 次），超限即作废，防暴力猜码；</li>
 *   <li>校验成功立即作废，防重放；</li>
 *   <li>验证码用 {@link SecureRandom} 生成，不可预测。</li>
 * </ul>
 */
@Service
public class SmsCodeService {

    private final SecurityProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();

    public SmsCodeService(SecurityProperties properties) {
        this.properties = properties;
    }

    /** 生成验证码并登记。同一手机号在发送间隔内再次请求会被限流。 */
    public String generate(String phone) {
        CodeEntry existing = codes.get(phone);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null && existing.lastSentAt != null
                && existing.lastSentAt.plusSeconds(properties.getSms().getSendIntervalSeconds()).isAfter(now)) {
            throw new BizException(ResultCode.RATE_LIMIT, 429, "发送过于频繁，请稍后再试");
        }
        String code = randomCode(properties.getSms().getCodeLength());
        CodeEntry entry = new CodeEntry();
        entry.code = code;
        entry.expiresAt = now.plusSeconds(properties.getSms().getCodeTtlSeconds());
        entry.lastSentAt = now;
        entry.attempts = 0;
        codes.put(phone, entry);
        return code;
    }

    /**
     * 校验验证码。成功立即作废；失败累计次数，超限作废。
     * 不存在或已过期都返回同一业务码，不泄露手机号是否发过码。
     */
    public boolean verify(String phone, String code) {
        CodeEntry entry = codes.get(phone);
        if (entry == null || entry.expiresAt.isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.SMS_CODE_INVALID, 401, "验证码不存在或已过期");
        }
        synchronized (entry) {
            if (entry.attempts >= properties.getSms().getMaxVerifyAttempts()) {
                codes.remove(phone);
                throw new BizException(ResultCode.SMS_CODE_INVALID, 401, "验证码错误次数过多，请重新获取");
            }
            if (entry.code.equals(code)) {
                codes.remove(phone);
                return true;
            }
            entry.attempts++;
            return false;
        }
    }

    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }

    private static class CodeEntry {
        private String code;
        private LocalDateTime expiresAt;
        private LocalDateTime lastSentAt;
        private int attempts;
    }
}
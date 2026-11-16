package com.allst.springboot.security;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.security.sms.SmsCodeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证码服务单元测试。覆盖生成、校验、试错上限、成功作废与发送间隔限流。
 * 不依赖外部短信通道，只验证内存验证码状态机。
 */
class SmsCodeServiceTest {

    private static SmsCodeService service() {
        SecurityProperties properties = new SecurityProperties();
        properties.getSms().setCodeLength(6);
        properties.getSms().setCodeTtlSeconds(300);
        properties.getSms().setMaxVerifyAttempts(5);
        properties.getSms().setSendIntervalSeconds(60);
        return new SmsCodeService(properties);
    }

    @Test
    void generatedCodeVerifiesOnce() {
        SmsCodeService service = service();
        String code = service.generate("13800000001");
        assertEquals(6, code.length());
        assertTrue(service.verify("13800000001", code));
        // 成功后立即作废，重复使用抛业务异常（防重放）
        BizException ex = assertThrows(BizException.class, () -> service.verify("13800000001", code));
        assertEquals(ResultCode.SMS_CODE_INVALID, ex.getCode());
    }

    @Test
    void wrongCodeCountsAndLocksAfterLimit() {
        SmsCodeService service = service();
        service.generate("13800000002");
        for (int i = 0; i < 5; i++) {
            assertFalse(service.verify("13800000002", "000000"));
        }
        // 超过试错上限后验证码作废，即使正确码也验不过
        BizException ex = assertThrows(BizException.class, () -> service.verify("13800000002", "000000"));
        assertEquals(ResultCode.SMS_CODE_INVALID, ex.getCode());
    }

    @Test
    void verifyWithoutGenerateRejected() {
        SmsCodeService service = service();
        BizException ex = assertThrows(BizException.class, () -> service.verify("13800000003", "123456"));
        assertEquals(ResultCode.SMS_CODE_INVALID, ex.getCode());
    }

    @Test
    void sendIntervalLimitsResend() {
        SmsCodeService service = service();
        service.generate("13800000004");
        // 60 秒内再次发送被限流
        BizException ex = assertThrows(BizException.class, () -> service.generate("13800000004"));
        assertEquals(ResultCode.RATE_LIMIT, ex.getCode());
    }
}
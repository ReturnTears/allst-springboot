package com.allst.springboot.security.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * 阿里云短信发送。{@code security.sms.provider=aliyun} 时装配。
 * <p>
 * AccessKey 从环境变量 {@code ALIBABA_CLOUD_ACCESS_KEY_ID} / {@code ALIBABA_CLOUD_ACCESS_KEY_SECRET} 读取，
 * 不硬编码、不进配置文件。签名与模板在 application.yml 的 security.sms 下配置。
 * 模板变量约定为 {@code {"code":"验证码"}}，需在阿里云控制台申请对应模板。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "security.sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private final SecurityProperties properties;
    private Client client;

    public AliyunSmsSender(SecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws Exception {
        String accessKeyId = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new IllegalStateException(
                    "启用阿里云短信需设置环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID 与 ALIBABA_CLOUD_ACCESS_KEY_SECRET");
        }
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = properties.getSms().getEndpoint();
        this.client = new Client(config);
    }

    @Override
    public void send(String phone, String code) {
        SecurityProperties.Sms sms = properties.getSms();
        if (!StringUtils.hasText(sms.getSignName()) || !StringUtils.hasText(sms.getTemplateCode())) {
            throw new BizException(ResultCode.SERVER, 500, "阿里云短信签名或模板未配置");
        }
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(sms.getSignName())
                .setTemplateCode(sms.getTemplateCode())
                .setTemplateParam("{\"code\":\"" + code + "\"}");
        try {
            SendSmsResponse response = client.sendSms(request);
            String bodyCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equalsIgnoreCase(bodyCode)) {
                log.warn("阿里云短信发送失败: code={}", bodyCode);
                throw new BizException(ResultCode.SERVER, 502, "短信发送失败");
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("阿里云短信调用异常: {}", ex.getClass().getSimpleName());
            throw new BizException(ResultCode.SERVER, 502, "短信发送失败");
        }
    }
}
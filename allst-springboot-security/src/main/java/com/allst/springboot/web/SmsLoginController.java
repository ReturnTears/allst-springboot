package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.security.sms.SmsCodeService;
import com.allst.springboot.security.sms.SmsLoginService;
import com.allst.springboot.security.sms.SmsSender;
import com.allst.springboot.web.dto.SmsLoginRequest;
import com.allst.springboot.web.dto.SmsSendRequest;
import com.allst.springboot.web.dto.TokenView;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 短信登录入口，挂在 {@code /api/**} 链上，两个地址都 permitAll。
 * <p>
 * send：生成验证码 → 限流 → 发送。演示模式（provider=log）把验证码回显到响应，方便本机调试与测试；
 * 阿里云模式不回显，避免泄露。响应不暴露手机号是否已注册，防账号探测。
 * <p>
 * login：交给 SmsLoginService 校验码、自动注册、签令牌。验证码错误时补发认证失败事件，
 * 复用 AuthenticationAuditListener 的审计与锁定。
 */
@RestController
@RequestMapping("/api/auth/sms")
public class SmsLoginController {

    private final SmsCodeService codes;
    private final SmsSender sender;
    private final SmsLoginService loginService;
    private final SecurityProperties properties;
    private final ApplicationEventPublisher events;

    public SmsLoginController(SmsCodeService codes, SmsSender sender, SmsLoginService loginService,
                              SecurityProperties properties, ApplicationEventPublisher events) {
        this.codes = codes;
        this.sender = sender;
        this.loginService = loginService;
        this.properties = properties;
        this.events = events;
    }

    @PostMapping("/send")
    public ApiResult<Map<String, Object>> send(@Valid @RequestBody SmsSendRequest request) {
        if (!properties.getSms().isEnabled()) {
            throw new BizException(ResultCode.NOT_ENABLED, 404, "短信登录未启用");
        }
        String code = codes.generate(request.getPhone());
        sender.send(request.getPhone(), code);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sent", true);
        // 仅演示模式回显验证码；生产（aliyun）不回显。
        if ("log".equalsIgnoreCase(properties.getSms().getProvider())) {
            body.put("code", code);
        }
        return ApiResult.ok(body);
    }

    @PostMapping("/login")
    public ApiResult<TokenView> login(@Valid @RequestBody SmsLoginRequest request) {
        if (!properties.getSms().isEnabled()) {
            throw new BizException(ResultCode.NOT_ENABLED, 404, "短信登录未启用");
        }
        try {
            return ApiResult.ok(loginService.login(request.getPhone(), request.getCode()));
        } catch (BizException ex) {
            if (ex.getCode() == ResultCode.SMS_CODE_INVALID) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(request.getPhone(), null);
                events.publishEvent(new AuthenticationFailureBadCredentialsEvent(authentication,
                        new BadCredentialsException("Bad credentials")));
            }
            throw ex;
        }
    }
}
package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.qr.QrLoginService;
import com.allst.springboot.security.qr.QrLoginService.QrStatus;
import com.allst.springboot.security.qr.QrLoginService.QrTicket;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.web.dto.QrRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 扫码登录入口，挂在 {@code /api/**} 链上。
 * <p>
 * create / status 是 permitAll（qrId 本身就是凭证）；scan / confirm / cancel 不在放行名单里，
 * 落到链末尾的 {@code anyRequest().authenticated()}，由 JwtAuthenticationFilter 校验手机令牌。
 * 扫码不认证手机，而是把手机已有的身份委托给 PC。
 */
@RestController
@RequestMapping("/api/auth/qr")
public class QrLoginController {

    private final QrLoginService qr;

    public QrLoginController(QrLoginService qr) {
        this.qr = qr;
    }

    /** PC 创建二维码。 */
    @GetMapping("/create")
    public ApiResult<QrTicket> create() {
        return ApiResult.ok(qr.create());
    }

    /** PC 轮询状态；CONFIRMED 那一次返回令牌并立即作废二维码。 */
    @GetMapping("/status")
    public ApiResult<QrStatus> status(@RequestParam String qrId) {
        return ApiResult.ok(qr.status(qrId));
    }

    /** 手机扫码，把二维码绑定到当前手机用户。 */
    @PostMapping("/scan")
    public ApiResult<QrStatus> scan(@Valid @RequestBody QrRequest request, Authentication authentication) {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        qr.scan(request.getQrId(), user.getId());
        return ApiResult.ok(qr.peek(request.getQrId()));
    }

    /** 手机确认，为该用户签发 JWT 暂存，等待 PC 取走。 */
    @PostMapping("/confirm")
    public ApiResult<QrStatus> confirm(@Valid @RequestBody QrRequest request, Authentication authentication) {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        qr.confirm(request.getQrId(), user.getId(), user.getUsername());
        return ApiResult.ok(qr.peek(request.getQrId()));
    }

    /** 手机取消。 */
    @PostMapping("/cancel")
    public ApiResult<String> cancel(@Valid @RequestBody QrRequest request, Authentication authentication) {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        qr.cancel(request.getQrId(), user.getId());
        return ApiResult.ok("已取消");
    }
}
package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.password.PasswordPolicy;
import com.allst.springboot.security.password.PasswordService;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.security.user.SecurityUserDetailsService;
import com.allst.springboot.web.dto.PasswordChangeRequest;
import com.allst.springboot.web.dto.PasswordPolicyView;
import com.allst.springboot.web.dto.TokenView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 查看口令规则、本人改密。改密后旧访问令牌作废，并返回新的一对令牌。见 docs/05-password-management.md。
 */
@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordService passwordService;
    private final JwtTokenService tokens;
    private final SecurityUserDetailsService users;
    private final SecurityProperties properties;

    public PasswordController(PasswordService passwordService, JwtTokenService tokens,
                              SecurityUserDetailsService users, SecurityProperties properties) {
        this.passwordService = passwordService;
        this.tokens = tokens;
        this.users = users;
        this.properties = properties;
    }

    @GetMapping("/policy")
    public ApiResult<PasswordPolicyView> policy() {
        PasswordPolicyView view = new PasswordPolicyView(
                PasswordPolicy.MIN_LENGTH,
                PasswordPolicy.MAX_LENGTH,
                properties.getPassword().getHistorySize(),
                properties.getPassword().getMaxAgeDays(),
                "至少包含大写、小写、数字和符号，不能包含用户名，不能与最近使用过的口令相同");
        return ApiResult.ok(view);
    }

    @PostMapping("/change")
    public ApiResult<TokenView> change(@Valid @RequestBody PasswordChangeRequest request, Authentication authentication,
                                       @RequestHeader("Authorization") String authorization) {
        SecurityUser current = (SecurityUser) authentication.getPrincipal();
        passwordService.changeOwn(current.getId(), request.getOldPassword(), request.getNewPassword());
        tokens.revokeAccess(authorization);
        tokens.revokeAllRefresh(current.getId());
        SecurityUser fresh = (SecurityUser) users.loadUserByUsername(current.getUsername());
        return ApiResult.ok(tokens.issue(fresh));
    }
}

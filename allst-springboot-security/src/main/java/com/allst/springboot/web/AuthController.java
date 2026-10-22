package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.LdapBindClient;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.password.PasswordService;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.security.user.SecurityUserDetailsService;
import com.allst.springboot.web.dto.ExpiredPasswordRequest;
import com.allst.springboot.web.dto.LoginRequest;
import com.allst.springboot.web.dto.LogoutRequest;
import com.allst.springboot.web.dto.RefreshRequest;
import com.allst.springboot.web.dto.TokenView;
import com.allst.springboot.web.dto.UserView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * JWT 登录、刷新、退出。见 docs/01-authentication.md、docs/09-jwt.md。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;
    private final PasswordService passwordService;
    private final SecurityUserDetailsService users;
    private final ObjectProvider<LdapBindClient> ldap;
    private final ApplicationEventPublisher events;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService tokens,
                          PasswordService passwordService, SecurityUserDetailsService users,
                          ObjectProvider<LdapBindClient> ldap, ApplicationEventPublisher events) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
        this.passwordService = passwordService;
        this.users = users;
        this.ldap = ldap;
        this.events = events;
    }

    @PostMapping("/login")
    public ApiResult<TokenView> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        return ApiResult.ok(tokens.issue((SecurityUser) authentication.getPrincipal()));
    }

    @PostMapping("/refresh")
    public ApiResult<TokenView> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResult.ok(tokens.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<String> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody(required = false) LogoutRequest request) {
        tokens.revokeAccess(authorization);
        if (request != null) {
            tokens.revokeRefresh(request.getRefreshToken());
        }
        return ApiResult.ok("已退出");
    }

    @GetMapping("/me")
    public ApiResult<UserView> me(Authentication authentication) {
        return ApiResult.ok(UserView.from((SecurityUser) authentication.getPrincipal()));
    }

    @PostMapping("/password-expired")
    public ApiResult<String> changeExpired(@Valid @RequestBody ExpiredPasswordRequest request) {
        try {
            passwordService.changeWithOldPassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
        } catch (BizException ex) {
            if (ex.getCode() == ResultCode.BAD_CREDENTIALS) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(request.getUsername(), null);
                events.publishEvent(new AuthenticationFailureBadCredentialsEvent(authentication,
                        new BadCredentialsException("Bad credentials")));
            }
            throw ex;
        }
        return ApiResult.ok("密码已更新，请重新登录");
    }

    @PostMapping("/ldap")
    public ApiResult<TokenView> ldap(@Valid @RequestBody LoginRequest request) {
        LdapBindClient client = ldap.getIfAvailable();
        if (client == null) {
            throw new BizException(ResultCode.NOT_ENABLED, 404, "LDAP 协议未启用");
        }
        Authentication bound;
        try {
            bound = client.bind(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(request.getUsername(), null);
            events.publishEvent(new AuthenticationFailureBadCredentialsEvent(authentication,
                    new BadCredentialsException("Bad credentials")));
            throw new BizException(ResultCode.BAD_CREDENTIALS, 401, "用户名或密码错误");
        }
        try {
            SecurityUser user = (SecurityUser) users.loadUserByUsername(bound.getName());
            return ApiResult.ok(tokens.issue(user));
        } catch (UsernameNotFoundException ex) {
            throw new BizException(ResultCode.EXTERNAL_NOT_BOUND, 401, "外部身份已认证，但本地没有同名账号");
        }
    }
}

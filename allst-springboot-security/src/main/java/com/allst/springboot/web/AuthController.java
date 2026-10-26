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
 * 身份入口。这些地址都在 {@code /api/**} 链上。
 * <p>
 * {@code POST /api/auth/login} 的调用链：
 * <pre>
 * LoginProtectionFilter          维护开关、按 IP 限流
 *   → apiChain                   这个地址是 permitAll，此时还没有令牌
 *   → AuthController.login
 *   → AuthenticationManager
 *   → DaoAuthenticationProvider
 *   → SecurityUserDetailsService 查账号，BCrypt 比对口令
 *   → AuthenticationAuditListener 写登录审计，并累计失败次数
 *   → JwtTokenService.issue      签发访问令牌和刷新令牌
 * </pre>
 * 之后访问 {@code GET /api/auth/me} 时，链路上多了 {@code JwtAuthenticationFilter}：
 * 它验签、查注销名单、再按用户名读一次数据库，把用户放进 SecurityContext，控制器才能拿到 {@code Authentication}。
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

    /**
     * 用用户名和口令换一对令牌。这里不自己查库，是为了走 Spring Security 统一的账号状态判断
     * （停用、锁定都会变成对应异常），审计监听器也能收到同一次认证事件。
     */
    @PostMapping("/login")
    public ApiResult<TokenView> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        return ApiResult.ok(tokens.issue((SecurityUser) authentication.getPrincipal()));
    }

    /**
     * 访问令牌快过期时，用刷新令牌换新的一对。旧刷新令牌会立刻作废，防止同一张刷新令牌被反复使用。
     * 这个地址同样 permitAll，因为过期的访问令牌不应该再被拿来调用它。
     */
    @PostMapping("/refresh")
    public ApiResult<TokenView> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResult.ok(tokens.refresh(request.getRefreshToken()));
    }

    /**
     * 退出只注销这次请求头里的访问令牌，以及 body 里带来的那张刷新令牌。
     * 更早签发、还没过期的访问令牌不会被这一个请求连带作废，所以退出后客户端应丢掉本地保存的全部令牌。
     */
    @PostMapping("/logout")
    public ApiResult<String> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody(required = false) LogoutRequest request) {
        tokens.revokeAccess(authorization);
        if (request != null) {
            tokens.revokeRefresh(request.getRefreshToken());
        }
        return ApiResult.ok("已退出");
    }

    /**
     * 返回当前令牌对应的人。{@code authentication} 不是前端传来的，是过滤器校验通过后放进安全上下文的。
     */
    @GetMapping("/me")
    public ApiResult<UserView> me(Authentication authentication) {
        return ApiResult.ok(UserView.from((SecurityUser) authentication.getPrincipal()));
    }

    /**
     * 密码过期后，正常登录拿到的令牌几乎调不了别的接口。这个地址允许在没有令牌时，拿旧口令换新口令。
     * 旧口令错误时补发一次失败事件，这样也会累计锁定次数，避免这个口子被拿去无限试口令。
     */
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

    /**
     * LDAP 只证明「目录里的口令是对的」。通过后仍用同名的本地账号签发 JWT，角色也以本地库为准。
     * 不把 LDAP 提供者挂进全局 AuthenticationManager，是为了避免普通登录失败后又去目录上试一遍。
     */
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

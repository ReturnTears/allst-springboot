package com.allst.springboot.security.jwt;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.domain.entity.RefreshToken;
import com.allst.springboot.domain.entity.TokenBlacklist;
import com.allst.springboot.domain.repo.RefreshTokenRepository;
import com.allst.springboot.domain.repo.TokenBlacklistRepository;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.security.user.SecurityUserDetailsService;
import com.allst.springboot.web.dto.TokenView;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 签发和校验 JWT。签名算法是 HS256，密钥在服务端，调用方改任何一个字段都会验签失败。
 * <p>
 * 访问令牌不落库，靠签名和过期时间。这样校验时不必每次查签发记录。
 * 刷新令牌只把编号 jti 存进 {@code sec_refresh_token}，原始字符串不落库；轮换或退出时把 revoked 设为 true。
 * 退出的访问令牌 jti 写入 {@code sec_token_blacklist}，过滤器每次都会查这张表。
 */
@Service
public class JwtTokenService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecurityProperties properties;
    private final RefreshTokenRepository refreshTokens;
    private final TokenBlacklistRepository blacklist;
    private final SecurityUserDetailsService userDetailsService;
    private final List<TokenCustomizer> customizers;
    private SecretKey key;

    public JwtTokenService(SecurityProperties properties, RefreshTokenRepository refreshTokens,
                           TokenBlacklistRepository blacklist, SecurityUserDetailsService userDetailsService,
                           List<TokenCustomizer> customizers) {
        this.properties = properties;
        this.refreshTokens = refreshTokens;
        this.blacklist = blacklist;
        this.userDetailsService = userDetailsService;
        this.customizers = customizers;
    }

    /** HS256 要求密钥至少 32 字节。启动时就失败，避免带着过短的密钥跑起来后所有令牌都能被伪造。 */
    @PostConstruct
    public void init() {
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("security.jwt.secret 长度至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    /**
     * 同时发两张令牌。访问令牌给业务接口，刷新令牌只给 {@code /api/auth/refresh}。
     * 角色写进访问令牌只是给前端展示；授权时过滤器仍会再查数据库。
     * {@link TokenCustomizer} 在签名前被调用，新增声明时实现这个接口即可，不用改本方法。
     */
    @Transactional
    public TokenView issue(SecurityUser user) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        Date now = new Date();
        Date accessExp = new Date(now.getTime() + properties.getJwt().getAccessTokenMinutes() * 60_000L);
        Date refreshExp = new Date(now.getTime() + properties.getJwt().getRefreshTokenDays() * 86_400_000L);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("uid", user.getId());
        claims.put("roles", user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(code -> code.startsWith("ROLE_"))
                .collect(Collectors.toList()));
        for (TokenCustomizer customizer : customizers) {
            customizer.customize(claims, user);
        }

        String access = build(user.getUsername(), accessJti, TYPE_ACCESS, now, accessExp, claims);
        String refresh = build(user.getUsername(), refreshJti, TYPE_REFRESH, now, refreshExp, Map.of("uid", user.getId()));

        RefreshToken stored = new RefreshToken();
        stored.setUserId(user.getId());
        stored.setJti(refreshJti);
        stored.setExpiresAt(toLocal(refreshExp));
        stored.setRevoked(false);
        stored.setCreatedAt(toLocal(now));
        refreshTokens.save(stored);

        return new TokenView(access, refresh, "Bearer", properties.getJwt().getAccessTokenMinutes() * 60L,
                user.isPasswordExpired());
    }

    /** 验签、核对签发者，并允许 30 秒时钟偏差，避免两台机器时间略有差别时把合法令牌判过期。 */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(properties.getJwt().getIssuer())
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 刷新时先作废旧的 jti，再签发新的一对。中途失败的话用户需要重新登录，这是为了不让一张刷新令牌被重复使用。
     * 签发前再读一次账号，停用或锁定的用户不能靠旧刷新令牌继续换新证。
     */
    @Transactional
    public TokenView refresh(String refreshToken) {
        Claims claims = parseOrUnauthorized(refreshToken);
        if (!TYPE_REFRESH.equals(claims.get("typ", String.class))) {
            throw new BizException(ResultCode.UNAUTHORIZED, 401, "不是刷新令牌");
        }
        RefreshToken stored = refreshTokens.findByJti(claims.getId())
                .orElseThrow(() -> new BizException(ResultCode.UNAUTHORIZED, 401, "刷新令牌已失效"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.UNAUTHORIZED, 401, "刷新令牌已失效");
        }
        stored.setRevoked(true);
        refreshTokens.save(stored);
        SecurityUser user = (SecurityUser) userDetailsService.loadUserByUsername(claims.getSubject());
        if (!user.isEnabled() || !user.isAccountNonLocked() || !user.isAccountNonExpired()) {
            throw new BizException(ResultCode.DISABLED, 401, "账号不可用");
        }
        return issue(user);
    }

    @Transactional
    public void revokeAccess(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        try {
            Claims claims = parse(stripBearer(accessToken));
            if (!TYPE_ACCESS.equals(claims.get("typ", String.class)) || claims.getId() == null) {
                return;
            }
            TokenBlacklist row = new TokenBlacklist();
            row.setJti(claims.getId());
            row.setExpiresAt(toLocal(claims.getExpiration()));
            blacklist.save(row);
        } catch (JwtException ex) {
            // 退出时令牌已经坏了也算退出完成，不把异常抛给调用方。
        }
    }

    @Transactional
    public void revokeRefresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            Claims claims = parse(refreshToken);
            refreshTokens.findByJti(claims.getId()).ifPresent(stored -> {
                stored.setRevoked(true);
                refreshTokens.save(stored);
            });
        } catch (JwtException ex) {
            // 退出不因令牌损坏而失败。
        }
    }

    /** 改密时调用。该用户名下尚未作废的刷新令牌全部失效，只保留这次改密新签发的那一张。 */
    @Transactional
    public void revokeAllRefresh(Long userId) {
        refreshTokens.revokeByUserId(userId);
    }

    public boolean accessRevoked(String jti) {
        return jti != null && blacklist.existsById(jti);
    }

    public String stripBearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return header;
    }

    private Claims parseOrUnauthorized(String token) {
        try {
            return parse(token);
        } catch (JwtException ex) {
            throw new BizException(ResultCode.UNAUTHORIZED, 401, "令牌无效或已过期");
        }
    }

    private String build(String subject, String jti, String type, Date now, Date exp, Map<String, Object> claims) {
        return Jwts.builder()
                .setIssuer(properties.getJwt().getIssuer())
                .setSubject(subject)
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("typ", type)
                .addClaims(claims)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private LocalDateTime toLocal(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}

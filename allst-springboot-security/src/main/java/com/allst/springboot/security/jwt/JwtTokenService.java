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
 * 签发和校验 HS256 JWT。访问令牌无状态，刷新令牌靠数据库里的 jti 轮换和作废。见 docs/09-jwt.md。
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

    @PostConstruct
    public void init() {
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("security.jwt.secret 长度至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

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

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(properties.getJwt().getIssuer())
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

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

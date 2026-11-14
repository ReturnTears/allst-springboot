package com.allst.springboot.security.qr;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.web.dto.TokenView;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫码登录状态机。PC 建码 → 手机扫码（绑定手机用户）→ 手机确认（为该用户签 JWT 暂存）→ PC 轮询一次性取走。
 * <p>
 * 扫码不认证手机，而是把手机已有的 JWT 身份委托给 PC：scan/confirm 都由已登录的手机调用，
 * 服务端只校验「这个二维码是不是绑定到当前手机用户」。
 * <p>
 * 存储是单实例内存（{@link ConcurrentHashMap}）。多实例部署时 PC 轮询和手机确认可能打到不同机器，
 * 必须换成 Redis 等共享存储并带 TTL，否则状态对不上。
 */
@Service
public class QrLoginService {

    /** 二维码状态。CONFIRMED 后 PC 取走令牌即转 EXPIRED，保证一次性。 */
    public enum QrState {
        WAITING, SCANNED, CONFIRMED, CANCELED, EXPIRED
    }

    private final SecurityProperties properties;
    private final JwtTokenService tokens;
    private final UserDetailsService users;
    private final Map<String, QrEntry> entries = new ConcurrentHashMap<>();

    public QrLoginService(SecurityProperties properties, JwtTokenService tokens, UserDetailsService users) {
        this.properties = properties;
        this.tokens = tokens;
        this.users = users;
    }

    /** PC 创建二维码。qrId 用 UUID 防猜测，短 TTL 防长期有效。 */
    public QrTicket create() {
        purgeExpired();
        String qrId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(properties.getQr().getTtlSeconds());
        entries.put(qrId, new QrEntry(expiresAt));
        return new QrTicket(qrId, QrState.WAITING, expiresAt);
    }

    /**
     * PC 轮询状态。只有 CONFIRMED 才返回令牌，且取走后立即作废二维码（一次性，防重放）。
     * 未确认时只暴露状态，不泄露任何用户信息。
     */
    public QrStatus status(String qrId) {
        QrEntry entry = entries.get(qrId);
        if (entry == null) {
            throw new BizException(ResultCode.QR_INVALID, 404, "二维码不存在或已失效");
        }
        synchronized (entry) {
            if (isExpired(entry)) {
                entries.remove(qrId);
                return new QrStatus(QrState.EXPIRED, null);
            }
            if (entry.state == QrState.CONFIRMED) {
                TokenView token = entry.token;
                entry.token = null;
                entry.state = QrState.EXPIRED;
                entries.remove(qrId);
                return new QrStatus(QrState.CONFIRMED, token);
            }
            return new QrStatus(entry.state, null);
        }
    }

    /** 手机扫码：把二维码绑定到当前手机用户。只有 WAITING 可扫。 */
    public void scan(String qrId, Long userId) {
        QrEntry entry = active(qrId);
        synchronized (entry) {
            if (entry.state != QrState.WAITING) {
                throw new BizException(ResultCode.QR_INVALID, 400, "二维码状态不允许扫码");
            }
            entry.userId = userId;
            entry.state = QrState.SCANNED;
        }
    }

    /**
     * 手机确认：校验二维码绑定的就是当前手机用户，再为该用户签发 JWT 暂存。
     * 绑定校验防止替别人确认。
     */
    public void confirm(String qrId, Long userId, String username) {
        QrEntry entry = active(qrId);
        synchronized (entry) {
            if (entry.state != QrState.SCANNED) {
                throw new BizException(ResultCode.QR_INVALID, 400, "二维码状态不允许确认");
            }
            if (!userId.equals(entry.userId)) {
                throw new BizException(ResultCode.FORBIDDEN, 403, "不能确认他人的二维码");
            }
            SecurityUser user = (SecurityUser) users.loadUserByUsername(username);
            entry.token = tokens.issue(user);
            entry.state = QrState.CONFIRMED;
        }
    }

    /** 手机取消。只有尚未确认的二维码可取消。 */
    public void cancel(String qrId, Long userId) {
        QrEntry entry = active(qrId);
        synchronized (entry) {
            if (entry.state == QrState.CONFIRMED) {
                throw new BizException(ResultCode.QR_INVALID, 400, "二维码已确认，无法取消");
            }
            if (entry.userId != null && !userId.equals(entry.userId)) {
                throw new BizException(ResultCode.FORBIDDEN, 403, "不能取消他人的二维码");
            }
            entry.state = QrState.CANCELED;
            entries.remove(qrId);
        }
    }

    private QrEntry active(String qrId) {
        QrEntry entry = entries.get(qrId);
        if (entry == null) {
            throw new BizException(ResultCode.QR_INVALID, 404, "二维码不存在或已失效");
        }
        synchronized (entry) {
            if (isExpired(entry)) {
                entries.remove(qrId);
                throw new BizException(ResultCode.QR_INVALID, 404, "二维码已过期");
            }
        }
        return entry;
    }

    private boolean isExpired(QrEntry entry) {
        return entry.state == QrState.EXPIRED || entry.expiresAt.isBefore(LocalDateTime.now());
    }

    private void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        entries.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
    }

    /** 建码返回给 PC 的票据。 */
    public static class QrTicket {
        private final String qrId;
        private final QrState state;
        private final LocalDateTime expiresAt;

        public QrTicket(String qrId, QrState state, LocalDateTime expiresAt) {
            this.qrId = qrId;
            this.state = state;
            this.expiresAt = expiresAt;
        }

        public String getQrId() {
            return qrId;
        }

        public QrState getState() {
            return state;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
    }

    /** 轮询返回给 PC 的状态。token 仅在 CONFIRMED 那一次非空。 */
    public static class QrStatus {
        private final QrState state;
        private final TokenView token;

        public QrStatus(QrState state, TokenView token) {
            this.state = state;
            this.token = token;
        }

        public QrState getState() {
            return state;
        }

        public TokenView getToken() {
            return token;
        }
    }

    private static class QrEntry {
        private final LocalDateTime expiresAt;
        private QrState state = QrState.WAITING;
        private Long userId;
        private TokenView token;

        private QrEntry(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
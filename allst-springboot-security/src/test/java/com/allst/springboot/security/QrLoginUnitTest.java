package com.allst.springboot.security;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.domain.entity.RefreshToken;
import com.allst.springboot.domain.repo.RefreshTokenRepository;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.qr.QrLoginService;
import com.allst.springboot.security.qr.QrLoginService.QrState;
import com.allst.springboot.security.qr.QrLoginService.QrStatus;
import com.allst.springboot.security.qr.QrLoginService.QrTicket;
import com.allst.springboot.security.user.SecurityUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 扫码状态机单元测试。用真实的 JwtTokenService（内存仓库传 null，签发只用到密钥与属性），
 * 不 mock 框架行为，验证「建码 → 扫码 → 确认 → 一次性取走」与越权拒绝。
 */
class QrLoginUnitTest {

    private static SecurityUser phoneUser() {
        return new SecurityUser(1L, "alice", "n/a", "爱丽丝", true, true, true, false,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * 内存刷新令牌仓库替身。只实现签发用到的 save / findByJti，其余方法抛 UnsupportedOperationException。
     * 用动态代理避免手写 JpaRepository 的几十个方法，让状态机测试不依赖数据库。
     */
    private static RefreshTokenRepository inMemoryRefreshTokens() {
        Map<String, RefreshToken> store = new ConcurrentHashMap<>();
        AtomicLong ids = new AtomicLong();
        return (RefreshTokenRepository) Proxy.newProxyInstance(
                RefreshTokenRepository.class.getClassLoader(),
                new Class<?>[]{RefreshTokenRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "save":
                            RefreshToken token = (RefreshToken) args[0];
                            if (token.getId() == null) {
                                token.setId(ids.incrementAndGet());
                            }
                            store.put(token.getJti(), token);
                            return token;
                        case "findByJti":
                            return Optional.ofNullable(store.get((String) args[0]));
                        case "toString":
                            return "InMemoryRefreshTokenRepository";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static QrLoginService service() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret("qr-unit-test-secret-key-at-least-32-bytes");
        JwtTokenService tokens = new JwtTokenService(properties, inMemoryRefreshTokens(), null, null, List.of());
        tokens.init();
        return new QrLoginService(properties, tokens, username -> phoneUser());
    }

    @Test
    void stateMachineTransitions() {
        QrLoginService service = service();
        QrTicket ticket = service.create();
        assertEquals(QrState.WAITING, service.status(ticket.getQrId()).getState());

        service.scan(ticket.getQrId(), 1L);
        assertEquals(QrState.SCANNED, service.status(ticket.getQrId()).getState());

        service.confirm(ticket.getQrId(), 1L, "alice");
        QrStatus confirmed = service.status(ticket.getQrId());
        assertEquals(QrState.CONFIRMED, confirmed.getState());
        assertNotNull(confirmed.getToken());
    }

    @Test
    void tokenIsConsumedOnce() {
        QrLoginService service = service();
        QrTicket ticket = service.create();
        service.scan(ticket.getQrId(), 1L);
        service.confirm(ticket.getQrId(), 1L, "alice");
        assertNotNull(service.status(ticket.getQrId()).getToken());
        // 取走后状态转 EXPIRED，再轮询拿不到令牌
        QrStatus again = service.status(ticket.getQrId());
        assertEquals(QrState.EXPIRED, again.getState());
        assertNull(again.getToken());
    }

    @Test
    void confirmByOtherUserRejected() {
        QrLoginService service = service();
        QrTicket ticket = service.create();
        service.scan(ticket.getQrId(), 1L);
        BizException ex = assertThrows(BizException.class, () -> service.confirm(ticket.getQrId(), 2L, "auditor"));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void scanAfterConfirmRejected() {
        QrLoginService service = service();
        QrTicket ticket = service.create();
        service.scan(ticket.getQrId(), 1L);
        service.confirm(ticket.getQrId(), 1L, "alice");
        service.status(ticket.getQrId());
        BizException ex = assertThrows(BizException.class, () -> service.scan(ticket.getQrId(), 1L));
        assertEquals(ResultCode.QR_INVALID, ex.getCode());
    }

    @Test
    void unknownQrRejected() {
        QrLoginService service = service();
        BizException ex = assertThrows(BizException.class, () -> service.status("no-such-qr"));
        assertEquals(ResultCode.QR_INVALID, ex.getCode());
    }
}
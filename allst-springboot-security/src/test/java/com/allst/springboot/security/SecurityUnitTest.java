package com.allst.springboot.security;

import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.security.auth.SlidingWindowLimiter;
import com.allst.springboot.security.password.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUnitTest {

    @Test
    void passwordMustMixCharacterClasses() {
        assertThrows(BizException.class, () -> PasswordPolicy.check("bob", "password", List.of()));
        assertThrows(BizException.class, () -> PasswordPolicy.check("bob", "Bob@123", List.of()));
        PasswordPolicy.check("bob", "Reader@123", List.of());
    }

    @Test
    void denylistAndHistoryWindow() {
        assertThrows(BizException.class, () -> PasswordPolicy.check("bob", "Password1!", List.of("Password1!")));
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        assertTrue(limiter.allow("1.1.1.1 /api/auth/login", 2, Duration.ofMinutes(1)));
        assertTrue(limiter.allow("1.1.1.1 /api/auth/login", 2, Duration.ofMinutes(1)));
        assertFalse(limiter.allow("1.1.1.1 /api/auth/login", 2, Duration.ofMinutes(1)));
    }
}

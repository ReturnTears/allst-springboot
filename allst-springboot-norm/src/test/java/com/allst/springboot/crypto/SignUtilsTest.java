package com.allst.springboot.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SignUtilsTest {

    @Test
    void matchesCrossLanguageVector() {
        String canonical = SignUtils.canonical("post", "/api/v1/users", null, "{}",
                "demo-app", "1700000000000", "abc", null, null);
        assertEquals("POST\n/api/v1/users\n\n{}\ndemo-app\n1700000000000\nabc\n\n", canonical);
        assertEquals("2e6719548bfd2983190305d8b2f72afbfb1415082026d3afcbccd385a5461083",
                SignUtils.sign(canonical, "demo-secret"));
    }

    @Test
    void sortsQueryByKey() {
        assertEquals("a=1&b=2", SignUtils.normalizeQuery("b=2&a=1"));
    }

    @Test
    void differentSecretChangesSign() {
        String canonical = SignUtils.canonical("GET", "/api/health", "", "", "demo-app", "1", "n", "", "");
        assertNotEquals(SignUtils.sign(canonical, "demo-secret"), SignUtils.sign(canonical, "other-secret"));
    }
}

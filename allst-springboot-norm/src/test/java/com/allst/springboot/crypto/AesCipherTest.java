package com.allst.springboot.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesCipherTest {

    private static final String KEY = "1234567890abcdef";

    @Test
    void roundTrip() {
        String cipher = AesCipher.encrypt("13800138000", KEY);
        assertNotEquals("13800138000", cipher);
        assertEquals("13800138000", AesCipher.decrypt(cipher, KEY));
    }

    @Test
    void samePlaintextProducesDifferentCiphertext() {
        assertNotEquals(AesCipher.encrypt("13800138000", KEY), AesCipher.encrypt("13800138000", KEY));
    }

    @Test
    void wrongKeyCannotDecrypt() {
        String cipher = AesCipher.encrypt("13800138000", KEY);
        assertThrows(IllegalStateException.class, () -> AesCipher.decrypt(cipher, "abcdef1234567890"));
    }
}

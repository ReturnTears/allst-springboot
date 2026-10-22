package com.allst.springboot.crypto;

import com.allst.springboot.log.SensitiveTextMasker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesensitizeUtilsTest {

    @Test
    void masksCommonFields() {
        assertEquals("138****8000", DesensitizeUtils.mask("13800138000", DesensitizeType.PHONE));
        assertEquals("110****1234", DesensitizeUtils.mask("110101199001011234", DesensitizeType.ID_CARD));
        assertEquals("张*", DesensitizeUtils.mask("张三", DesensitizeType.NAME));
        assertEquals("d***@example.com", DesensitizeUtils.mask("demo01@example.com", DesensitizeType.EMAIL));
        assertEquals("6222****1234", DesensitizeUtils.mask("6222020202021234", DesensitizeType.BANK_CARD));
    }

    @Test
    void masksTextLog() {
        String masked = SensitiveTextMasker.mask("{\"phone\":\"13800138000\",\"idCard\":\"110101199001011234\"}");
        assertFalse(masked.contains("13800138000"));
        assertFalse(masked.contains("110101199001011234"));
    }

    @Test
    void truncateKeepsSuffixInsideLimit() {
        String value = "a".repeat(100);
        String truncated = SensitiveTextMasker.truncate(value, 20);
        assertTrue(truncated.length() <= 20);
        assertTrue(truncated.endsWith("(truncated)"));
    }
}

package com.allst.exercise;

import com.allst.exercise.common.Result;
import com.allst.exercise.model.Customers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证统一响应体 + 全局空值剔除。
 * 只加载 Jackson 自动配置 + JacksonConfig，不启动数据源，无需 MySQL 实例。
 * 用容器里真实的 ObjectMapper（带 NON_EMPTY 定制），不 mock。
 */
@SpringBootTest(classes = {JacksonAutoConfiguration.class, com.allst.exercise.config.JacksonConfig.class})
class ResultSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    /** null 与空字符串属性都应被移除。 */
    @Test
    void nullAndEmptyFieldsAreRemoved() throws Exception {
        Customers customers = new Customers();
        customers.setId(1L);
        customers.setName("Xiaohu");
        customers.setEmail("");        // 空字符串 -> 移除
        customers.setAddress(null);    // null -> 移除
        // city / country 未赋值 -> null -> 移除

        String json = objectMapper.writeValueAsString(Result.ok(customers));

        assertTrue(json.contains("\"code\":200"));
        assertTrue(json.contains("\"name\":\"Xiaohu\""));
        assertFalse(json.contains("email"), "空字符串属性应被移除");
        assertFalse(json.contains("address"), "null 属性应被移除");
        assertFalse(json.contains("city"), "null 属性应被移除");
        assertFalse(json.contains("country"), "null 属性应被移除");
    }

    /** data 为 null 时，data 属性也应被移除。 */
    @Test
    void nullDataIsRemoved() throws Exception {
        String json = objectMapper.writeValueAsString(Result.ok());
        assertTrue(json.contains("\"code\":200"));
        assertFalse(json.contains("data"), "data 为 null 应被移除");
    }
}
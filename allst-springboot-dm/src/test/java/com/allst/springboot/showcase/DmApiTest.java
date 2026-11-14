package com.allst.springboot.showcase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 层验收：文档里的 curl 都能跑通。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DmApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DemoRunner demoRunner;

    @Test
    @DisplayName("POST /api/dm/orders 返回完整 trace")
    void placeOrder() throws Exception {
        String body = "{\"buyer\":\"tester\",\"sku\":\"SKU-PHONE\",\"unitPrice\":199.00,"
                + "\"quantity\":1,\"vip\":true,\"payChannel\":\"ALIPAY\",\"channel\":\"DOMESTIC\","
                + "\"requestId\":\"API-" + UUID.randomUUID() + "\",\"riskScore\":10}";
        mvc.perform(post("/api/dm/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.trace").isArray())
                .andExpect(jsonPath("$.data.trace.length()").value(org.hamcrest.Matchers.greaterThan(15)));
    }

    @Test
    @DisplayName("参数校验失败时统一异常外观返回 400 业务码")
    void validationFailure() throws Exception {
        String body = "{\"buyer\":\"\",\"sku\":\"SKU-PHONE\",\"unitPrice\":199.00,"
                + "\"quantity\":1,\"payChannel\":\"ALIPAY\",\"requestId\":\"BAD-1\"}";
        mvc.perform(post("/api/dm/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("买家不能为空")));
    }

    @Test
    @DisplayName("GET /api/dm/patterns 返回 26 条场景对照")
    void patterns() throws Exception {
        mvc.perform(get("/api/dm/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pattern").value("单例 Singleton"))
                .andExpect(jsonPath("$.data", hasSize(26)));
    }

    @Test
    @DisplayName("GET /api/dm/demo/{pattern} 对 23 个模式都能返回")
    void allDemosRespond() throws Exception {
        for (String key : demoRunner.keys()) {
            mvc.perform(get("/api/dm/demo/" + key))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("GET /api/dm/demo/strategy 与 /api/dm/stats 返回可读结果")
    void strategyDemoAndStats() throws Exception {
        mvc.perform(get("/api/dm/demo/strategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.best").value("VIP"));
        mvc.perform(get("/api/dm/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules.VIP_DISCOUNT").exists())
                .andExpect(jsonPath("$.data.springAopProxy").exists());
        mvc.perform(get("/api/dm/orders").param("batchSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNos").isArray());
    }
}

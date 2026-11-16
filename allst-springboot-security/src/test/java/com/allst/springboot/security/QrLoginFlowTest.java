package com.allst.springboot.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 扫码登录流程测试。覆盖「PC 建码 → 手机扫码确认 → PC 轮询取令牌」整条链，
 * 以及 scan/confirm 必须带手机令牌、不能替他人确认。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:qr-flow;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class QrLoginFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pcCreatesQrThenPhoneConfirmsThenPcPollsToken() throws Exception {
        MvcResult created = mockMvc.perform(get("/api/auth/qr/create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("WAITING"))
                .andReturn();
        String qrId = read(created).path("data").path("qrId").asText();

        String phoneToken = login("alice", "Reader@123");
        mockMvc.perform(post("/api/auth/qr/scan").header("Authorization", "Bearer " + phoneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrId\":\"" + qrId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("SCANNED"));
        mockMvc.perform(post("/api/auth/qr/confirm").header("Authorization", "Bearer " + phoneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrId\":\"" + qrId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("CONFIRMED"));

        MvcResult polled = mockMvc.perform(get("/api/auth/qr/status").param("qrId", qrId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.token.accessToken").exists())
                .andReturn();
        String pcToken = read(polled).path("data").path("token").path("accessToken").asText();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pcToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));

        // 令牌一次性取走，再轮询状态为 EXPIRED 且无令牌
        mockMvc.perform(get("/api/auth/qr/status").param("qrId", qrId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("EXPIRED"));
    }

    @Test
    void scanAndConfirmRequirePhoneLogin() throws Exception {
        MvcResult created = mockMvc.perform(get("/api/auth/qr/create")).andReturn();
        String qrId = read(created).path("data").path("qrId").asText();
        mockMvc.perform(post("/api/auth/qr/scan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrId\":\"" + qrId + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotConfirmQrBoundToAnotherUser() throws Exception {
        MvcResult created = mockMvc.perform(get("/api/auth/qr/create")).andReturn();
        String qrId = read(created).path("data").path("qrId").asText();
        String alice = login("alice", "Reader@123");
        String auditor = login("auditor", "Audit@123");
        mockMvc.perform(post("/api/auth/qr/scan").header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrId\":\"" + qrId + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/qr/confirm").header("Authorization", "Bearer " + auditor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrId\":\"" + qrId + "\"}"))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("accessToken").asText();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
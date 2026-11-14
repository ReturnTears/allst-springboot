package com.allst.springboot.security;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.qr.QrLoginService;
import com.allst.springboot.security.qr.QrLoginService.QrState;
import com.allst.springboot.security.qr.QrLoginService.QrStatus;
import com.allst.springboot.security.qr.QrLoginService.QrTicket;
import com.allst.springboot.security.user.SecurityUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 扫码登录。单元测试覆盖状态机，流程测试覆盖「PC 建码 → 手机扫码确认 → PC 轮询取令牌」整条链。
 * 扫码不认证手机，而是把手机已有的 JWT 身份委托给 PC。
 */
class QrLoginTest {

    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:qr-flow;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
    @AutoConfigureMockMvc
    static class Flow {

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

            // 令牌一次性取走，再轮询拿不到令牌
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

    private static SecurityUser phoneUser() {
        return new SecurityUser(1L, "alice", "n/a", "爱丽丝", true, true, true, false,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static QrLoginService service() {
        SecurityProperties properties = new SecurityProperties();
        JwtTokenService tokens = new JwtTokenService(properties, null, null, null, List.of());
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
        assertNull(service.status(ticket.getQrId()).getToken());
    }

    @Test
    void confirmByOtherUserRejected() {
        QrLoginService service = service();
        QrTicket ticket = service.create();
        service.scan(ticket.getQrId(), 1L);
        assertThrows(BizException.class, () -> service.confirm(ticket.getQrId(), 2L, "auditor"));
    }

    @Test
    void scanAfterConfirmRejected() {
        QrLoginService service = service();
        QrTicket ticket = service.create();
        service.scan(ticket.getQrId(), 1L);
        service.confirm(ticket.getQrId(), 1L, "alice");
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
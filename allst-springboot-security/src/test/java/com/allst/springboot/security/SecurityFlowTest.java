package com.allst.springboot.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:security-flow;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
@AutoConfigureMockMvc
class SecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void jwtLoginThenCallProtectedApi() throws Exception {
        String access = login("admin", "Manage@123");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").exists());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void authorizationSeparatesAuditorAndUser() throws Exception {
        String alice = login("alice", "Reader@123");
        String auditor = login("auditor", "Audit@123");
        mockMvc.perform(get("/api/audit/logins").header("Authorization", "Bearer " + alice))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit/logins").header("Authorization", "Bearer " + auditor))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"Reader@123\",\"displayName\":\"新人\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRotatesAndLogoutRevokes() throws Exception {
        JsonNode token = loginNode("alice", "Reader@123");
        String access = token.get("accessToken").asText();
        String refresh = token.get("refreshToken").asText();
        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String newAccess = objectMapper.readTree(refreshed.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + objectMapper.readTree(refreshed.getResponse().getContentAsString())
                                .path("data").path("refreshToken").asText() + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isUnauthorized());
        // 退出只注销本次提交的访问令牌。刷新前发出的访问令牌在过期前仍然有效。
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
    }

    @Test
    void methodSecurityHonorsExpressionsAndIgnoresHierarchyForJsr250() throws Exception {
        String admin = login("admin", "Manage@123");
        String alice = login("alice", "Reader@123");
        String auditor = login("auditor", "Audit@123");
        mockMvc.perform(get("/api/method/admin").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/method/admin").header("Authorization", "Bearer " + alice))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/method/jsr250-user").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/method/jsr250-user").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/method/secured-auditor").header("Authorization", "Bearer " + auditor))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/method/secured-auditor").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionAndBasicAndPublicHello() throws Exception {
        mockMvc.perform(get("/hello/security")).andExpect(status().isOk());
        mockMvc.perform(get("/api/password/policy")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minLength").value(8));
        MvcResult login = mockMvc.perform(post("/session/login").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", "Reader@123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/session/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));
        mockMvc.perform(get("/basic/me").with(httpBasic("auditor", "Audit@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("auditor"));
    }

    private String login(String username, String password) throws Exception {
        return loginNode(username, password).get("accessToken").asText();
    }

    private JsonNode loginNode(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}

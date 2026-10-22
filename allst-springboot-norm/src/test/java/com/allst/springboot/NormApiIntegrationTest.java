package com.allst.springboot;

import com.allst.springboot.common.header.HeaderNames;
import com.allst.springboot.crypto.SignUtils;
import com.allst.springboot.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

@SpringBootTest
@AutoConfigureMockMvc
class NormApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ThreadPoolTaskExecutor normExecutor;

    @Test
    void healthDoesNotNeedSignature() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void wrongMethodUsesUnifiedResult() throws Exception {
        mockMvc.perform(post("/api/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    void rejectsForeignIpBeforeSignature() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(request -> {
                    request.setRemoteAddr("8.8.8.8");
                    return request;
                }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void rejectsMissingSignature() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void rejectsExpiredTimestampAndReplay() throws Exception {
        performSigned("GET", "/api/v1/users", "page=0&size=10", null, null, "1", "nonce-expired")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));

        String nonce = "nonce-" + UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        performSigned("GET", "/api/v1/users", "page=0&size=10", null, null, timestamp, nonce)
                .andExpect(status().isOk());
        performSigned("GET", "/api/v1/users", "page=0&size=10", null, null, timestamp, nonce)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40103));
    }

    @Test
    void rejectsOversizedPage() throws Exception {
        performSigned("GET", "/api/v1/users", "page=0&size=51", null, null, null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("每页条数")));
    }

    @Test
    void rejectsInvalidBody() throws Exception {
        performSigned("POST", "/api/v1/users", null, "{}", "idem-invalid-body", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名")));
    }

    @Test
    void createUserEncryptsAtRestAndMasksInResponse() throws Exception {
        String username = "u" + System.currentTimeMillis();
        String body = userJson(username, "张三", "13800138000", "110101199001011234");
        performSigned("POST", "/api/v1/users", null, body, "idem-" + username, null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.realName").value("张*"))
                .andExpect(jsonPath("$.data.phone").value("138****8000"))
                .andExpect(jsonPath("$.data.idCard").value("110****1234"))
                .andExpect(jsonPath("$.data.email").value(org.hamcrest.Matchers.containsString("***@")))
                .andExpect(jsonPath("$.data.createdBy").value("tester"));

        String storedPhone = jdbcTemplate.queryForObject(
                "select phone from norm_user where username = ?", String.class, username);
        assertNotEquals("13800138000", storedPhone);
        assertEquals("13800138000", userRepository.findByUsername(username).orElseThrow().getPhone());
        waitForLog(username);
    }

    @Test
    void repeatedIdempotentKeyDoesNotCreateAnotherUser() throws Exception {
        String username = "idem" + (System.currentTimeMillis() % 1000000000L);
        String body = userJson(username, "李四", "13800138001", "110101199002021234");
        String key = "idem-key-" + username;
        String first = performSigned("POST", "/api/v1/users", null, body, key, null, null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String second = performSigned("POST", "/api/v1/users", null, body, key, null, null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(first, second);
        assertTrue(userRepository.findByUsername(username).isPresent());
    }

    @Test
    void rateLimitReturns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            performSigned("GET", "/api/v1/demo/ping", null, null, null, null, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("pong"));
        }
        performSigned("GET", "/api/v1/demo/ping", null, null, null, null, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(42901));
    }

    @Test
    void batchOverLimitIsRejected() throws Exception {
        StringBuilder users = new StringBuilder("{\"users\":[");
        for (int i = 0; i < 101; i++) {
            if (i > 0) {
                users.append(',');
            }
            users.append(userJson("buser" + i, "测", String.format("138%08d", i), String.format("11010119900101%04d", i)));
        }
        users.append("]}");
        performSigned("POST", "/api/v1/users/batch", null, users.toString(), "batch-over-limit", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("批量条数")));
    }

    @Test
    void asyncReportReturnsCounts() throws Exception {
        MvcResult started = performSigned("GET", "/api/v1/reports/summary", null, null, null, null, null)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    private ResultActions performSigned(String method, String path, String query, String body, String idempotentKey,
                                        String timestamp, String nonce) throws Exception {
        String ts = timestamp == null ? String.valueOf(System.currentTimeMillis()) : timestamp;
        String once = nonce == null ? UUID.randomUUID().toString().replace("-", "") : nonce;
        String operator = "tester";
        String canonical = SignUtils.canonical(method, path, query, body == null ? "" : body,
                "demo-app", ts, once, idempotentKey, operator);
        String sign = SignUtils.sign(canonical, "demo-secret");
        String uri = query == null || query.isEmpty() ? path : path + "?" + query;
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder;
        switch (method) {
            case "POST":
                builder = post(uri);
                break;
            case "PUT":
                builder = put(uri);
                break;
            case "PATCH":
                builder = patch(uri);
                break;
            case "DELETE":
                builder = delete(uri);
                break;
            default:
                builder = get(uri);
                break;
        }
        builder.header(HeaderNames.APP_ID, "demo-app")
                .header(HeaderNames.TIMESTAMP, ts)
                .header(HeaderNames.NONCE, once)
                .header(HeaderNames.SIGN, sign)
                .header(HeaderNames.OPERATOR, operator);
        if (idempotentKey != null) {
            builder.header(HeaderNames.IDEMPOTENT_KEY, idempotentKey);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("UTF-8")
                    .content(body.getBytes(StandardCharsets.UTF_8));
        }
        return mockMvc.perform(builder);
    }

    private String userJson(String username, String realName, String phone, String idCard) {
        return "{\"username\":\"" + username + "\",\"realName\":\"" + realName
                + "\",\"phone\":\"" + phone + "\",\"email\":\"" + username + "@example.com\",\"idCard\":\"" + idCard + "\"}";
    }

    private void waitForLog(String username) throws Exception {
        normExecutor.getThreadPoolExecutor().awaitTermination(0, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 30; i++) {
            Long count = jdbcTemplate.queryForObject(
                    "select count(*) from norm_api_log where request_body like ?",
                    Long.class, "%" + username + "%");
            if (count != null && count > 0) {
                String body = jdbcTemplate.queryForObject(
                        "select request_body from norm_api_log where request_body like ? order by id desc",
                        String.class, "%" + username + "%");
                assertFalse(body.contains("13800138000"));
                assertTrue(body.contains(username));
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("访问日志没有落库");
    }
}

package com.allst.springboot.ddl;

import com.allst.springboot.ddl.meta.MetadataSeeder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DDL 接口的 HTTP 验收：能力矩阵、离线生成、库内生成、导出下载、路径安全。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DdlApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MetadataSeeder seeder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        seeder.seed();
    }

    @Test
    @DisplayName("GET /api/dm/ddl/dialects 返回 10 个方言能力矩阵")
    void dialects() throws Exception {
        mvc.perform(get("/api/dm/ddl/dialects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.data[?(@.name=='IMPALA')]").exists());
    }

    @Test
    @DisplayName("POST /seed 灌入 5 张多方言示例表")
    void seed() throws Exception {
        mvc.perform(post("/api/dm/ddl/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("POST /generate 从库内读元数据按方言生成")
    void generateFromDatabase() throws Exception {
        mvc.perform(post("/api/dm/ddl/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dialect\":\"IMPALA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tableCount").value(1))
                .andExpect(jsonPath("$.data.scripts[0].dialect").value("IMPALA"))
                .andExpect(jsonPath("$.data.scripts[0].sql", containsString("PARTITIONED BY")));
    }

    @Test
    @DisplayName("POST /generate 用请求体里的元数据离线生成，不读库")
    void generateOffline() throws Exception {
        String body = "{\"dialect\":\"MYSQL\",\"tables\":[{"
                + "\"tableName\":\"offline_t\",\"dialect\":\"MYSQL\","
                + "\"columns\":[{\"columnName\":\"id\",\"logicalType\":\"BIGINT\","
                + "\"primaryKey\":true,\"autoIncrement\":true,\"ordinal\":0}]}]}";
        mvc.perform(post("/api/dm/ddl/generate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scripts[0].sql", containsString("AUTO_INCREMENT")));
    }

    @Test
    @DisplayName("POST /export + GET /download 文件可导出并下载")
    void exportAndDownload() throws Exception {
        MvcResult result = mvc.perform(post("/api/dm/ddl/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dialect\":\"MYSQL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.files", hasSize(1)))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String fileName = root.path("data").path("files").get(0).path("fileName").asText();
        assertTrue(fileName.startsWith("ddl-MYSQL-"));

        mvc.perform(get("/api/dm/ddl/download/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(fileName)));
    }

    @Test
    @DisplayName("GET /files 列出已导出文件")
    void listFiles() throws Exception {
        mvc.perform(post("/api/dm/ddl/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dialect\":\"IMPALA\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/dm/ddl/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.not(hasSize(0))));
    }

    @Test
    @DisplayName("下载非法文件名被拒绝：Web 层规范化返回 404，或导出服务校验返回 400")
    void downloadRejectsTraversal() throws Exception {
        mvc.perform(get("/api/dm/ddl/download/{fileName}", "..%2Fsecret.sql"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", org.hamcrest.Matchers.isOneOf(400, 404)));
    }
}

package com.allst.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * MockMvc测试类
 *
 * AutoConfigureMockMvc专门用于做MockMvc，由spring-test提供， 依赖junit5
 * @author June
 * @since 2021年06月
 */
@SpringBootTest
@AutoConfigureMockMvc
public class MockMvcTests {

    @Autowired
    private MockMvc mockMvc;
    /**
     * MockMvc不依赖网络， 不依赖web服务，不需要启动web应用
     */
    @Test
    void testMockMvc() throws Exception {
        System.out.println(mockMvc);
        mockMvc.perform(
                MockMvcRequestBuilders.get("/basic/user/{id}", 2)
                .accept(MediaType.APPLICATION_JSON)// 设置响应的文本类型
                // .param(name, value)?name=xx&age=xx
                // 响应断言
        ).andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }
}

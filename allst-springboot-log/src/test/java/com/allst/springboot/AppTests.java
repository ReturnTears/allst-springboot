package com.allst.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
public class AppTests {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/myMM/str"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                // 响应断言
                .andExpect(MockMvcResultMatchers.content().string("Hello World"));
    }
}

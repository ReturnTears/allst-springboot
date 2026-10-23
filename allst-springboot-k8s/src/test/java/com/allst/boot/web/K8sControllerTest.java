package com.allst.boot.web;

import com.allst.boot.k8s.K8sQueryService;
import com.allst.boot.k8s.model.ClientStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HomeController.class, K8sController.class})
class K8sControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private K8sQueryService service;

    @Test
    void homeListsEntryPoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hello").value("/k8s/hello"))
                .andExpect(jsonPath("$.health").value("/actuator/health"));
    }

    @Test
    void helloAndInfoDoNotRequireCluster() throws Exception {
        when(service.clientStatus()).thenReturn(new ClientStatus(false, "kubeconfig", "allst-demo", null, "no config"));

        mockMvc.perform(get("/k8s/hello"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/k8s/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("allst-springboot-k8s"))
                .andExpect(jsonPath("$.kubernetes.clientReady").value(false))
                .andExpect(jsonPath("$.kubernetes.namespace").value("allst-demo"));
    }

    @Test
    void listsPods() throws Exception {
        when(service.listPods("allst-demo")).thenReturn(List.of());
        mockMvc.perform(get("/k8s/pods").param("namespace", "allst-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

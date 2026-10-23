package com.allst.boot.k8s;

import com.allst.boot.config.K8sProperties;
import com.allst.boot.k8s.model.ClientStatus;
import com.allst.boot.k8s.model.PodView;
import io.fabric8.kubernetes.api.model.PodBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class K8sQueryServiceTest {

    @Test
    void rejectsWriteOutsideDemoNamespace() {
        K8sQueryService service = serviceWithoutCluster();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.upsertConfigMap("kube-system", "demo-config", Map.of("color", "blue")));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void rejectsIllegalNamespaceBeforeCallingApi() {
        K8sQueryService service = serviceWithoutCluster();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.listPods("BAD_NAME"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void reportsMissingClient() {
        K8sQueryService service = serviceWithoutCluster();
        ClientStatus status = service.clientStatus();
        assertFalse(status.isClientReady());
        assertEquals("kubeconfig", status.getMode());
        assertEquals("allst-demo", status.getNamespace());
        assertThrows(K8sUnavailableException.class, service::listNamespaces);
    }

    @Test
    void readsPodSummaryFromModel() {
        PodView view = K8sQueryService.toPodView(new PodBuilder()
                .withNewMetadata().withName("web").withNamespace("allst-demo").endMetadata()
                .withNewSpec().withNodeName("node-a").endSpec()
                .withNewStatus().withPhase("Running").withPodIP("10.1.0.8").endStatus()
                .build());
        assertEquals("web", view.getName());
        assertEquals("allst-demo", view.getNamespace());
        assertEquals("Running", view.getPhase());
        assertEquals("node-a", view.getNodeName());
        assertEquals("10.1.0.8", view.getPodIp());
    }

    private static K8sQueryService serviceWithoutCluster() {
        K8sProperties properties = new K8sProperties();
        properties.setNamespace("allst-demo");
        return new K8sQueryService(new K8sClientProvider(null, "kubeconfig", "no config", properties));
    }
}

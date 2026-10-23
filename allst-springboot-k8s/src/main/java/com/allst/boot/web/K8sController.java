package com.allst.boot.web;

import com.allst.boot.k8s.K8sQueryService;
import com.allst.boot.k8s.model.ClientStatus;
import com.allst.boot.k8s.model.ClusterView;
import com.allst.boot.k8s.model.ConfigMapView;
import com.allst.boot.k8s.model.DeploymentView;
import com.allst.boot.k8s.model.NamespaceView;
import com.allst.boot.k8s.model.PodView;
import com.allst.boot.k8s.model.ServiceView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Hutu
 * @since 2026-10-23
 */
@RestController
@RequestMapping("/k8s")
public class K8sController {

    private final K8sQueryService service;

    @Value("${spring.application.name:allst-springboot-k8s}")
    private String applicationName;

    @Value("${demo.message:hello from local properties}")
    private String demoMessage;

    @Value("${POD_NAME:}")
    private String podName;

    public K8sController(K8sQueryService service) {
        this.service = service;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello Kubernetes!";
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        ClientStatus status = service.clientStatus();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application", applicationName);
        body.put("message", demoMessage);
        body.put("pod", podName);
        body.put("hostname", hostname());
        body.put("javaVersion", System.getProperty("java.version"));
        body.put("kubernetes", status);
        return body;
    }

    @GetMapping("/cluster")
    public ClusterView cluster() {
        return service.cluster();
    }

    @GetMapping("/namespaces")
    public List<NamespaceView> namespaces() {
        return service.listNamespaces();
    }

    @GetMapping("/pods")
    public List<PodView> pods(@RequestParam(required = false) String namespace) {
        return service.listPods(namespace);
    }

    @GetMapping("/pods/{name}/log")
    public Map<String, Object> podLog(@PathVariable String name,
                                      @RequestParam(required = false) String namespace,
                                      @RequestParam(required = false) Integer tailLines) {
        String shownNamespace = namespace == null || namespace.isBlank()
                ? service.clientStatus().getNamespace()
                : namespace;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("namespace", shownNamespace);
        body.put("log", service.podLog(namespace, name, tailLines));
        return body;
    }

    @GetMapping("/deployments")
    public List<DeploymentView> deployments(@RequestParam(required = false) String namespace) {
        return service.listDeployments(namespace);
    }

    @GetMapping("/services")
    public List<ServiceView> services(@RequestParam(required = false) String namespace) {
        return service.listServices(namespace);
    }

    @GetMapping("/configmaps")
    public List<ConfigMapView> configMaps(@RequestParam(required = false) String namespace) {
        return service.listConfigMaps(namespace);
    }

    @GetMapping("/configmaps/{name}")
    public ConfigMapView configMap(@PathVariable String name,
                                   @RequestParam(required = false) String namespace) {
        return service.getConfigMap(namespace, name);
    }

    @PutMapping("/configmaps/{name}")
    public ConfigMapView upsertConfigMap(@PathVariable String name,
                                         @RequestParam(required = false) String namespace,
                                         @RequestBody ConfigMapRequest request) {
        if (request == null || request.getData() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体需要 data 字段");
        }
        return service.upsertConfigMap(namespace, name, request.getData());
    }

    @DeleteMapping("/configmaps/{name}")
    public Map<String, Object> deleteConfigMap(@PathVariable String name,
                                               @RequestParam(required = false) String namespace) {
        boolean deleted = service.deleteConfigMap(namespace, name);
        String shownNamespace = namespace == null || namespace.isBlank()
                ? service.clientStatus().getNamespace()
                : namespace;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("namespace", shownNamespace);
        body.put("deleted", deleted);
        return body;
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }
}

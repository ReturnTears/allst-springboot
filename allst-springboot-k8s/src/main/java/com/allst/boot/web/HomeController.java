package com.allst.boot.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", "Hello Kubernetes!");
        body.put("hello", "/k8s/hello");
        body.put("info", "/k8s/info");
        body.put("cluster", "/k8s/cluster");
        body.put("namespaces", "/k8s/namespaces");
        body.put("pods", "/k8s/pods");
        body.put("deployments", "/k8s/deployments");
        body.put("services", "/k8s/services");
        body.put("configmaps", "/k8s/configmaps");
        body.put("health", "/actuator/health");
        return body;
    }
}

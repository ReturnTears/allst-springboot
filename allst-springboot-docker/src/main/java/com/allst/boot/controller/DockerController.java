package com.allst.boot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Hutu
 * @since 2024-08-03 下午 10:11
 */
@RestController
@RequestMapping("/docker")
public class DockerController {

    private final Environment environment;

    @Value("${spring.application.name:allst-springboot-docker}")
    private String applicationName;

    @Value("${server.port:8099}")
    private String port;

    public DockerController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello Docker!";
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application", applicationName);
        body.put("message", "Spring Boot is running");
        body.put("port", port);
        body.put("profiles", Arrays.asList(environment.getActiveProfiles()));
        body.put("hostname", hostname());
        body.put("javaVersion", System.getProperty("java.version"));
        return body;
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }
}

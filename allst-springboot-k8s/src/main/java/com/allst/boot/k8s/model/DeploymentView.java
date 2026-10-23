package com.allst.boot.k8s.model;

import java.util.List;

public class DeploymentView {

    private final String name;
    private final String namespace;
    private final Integer replicas;
    private final Integer readyReplicas;
    private final Integer availableReplicas;
    private final List<String> images;

    public DeploymentView(String name, String namespace, Integer replicas, Integer readyReplicas,
                          Integer availableReplicas, List<String> images) {
        this.name = name;
        this.namespace = namespace;
        this.replicas = replicas;
        this.readyReplicas = readyReplicas;
        this.availableReplicas = availableReplicas;
        this.images = images;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public Integer getReadyReplicas() {
        return readyReplicas;
    }

    public Integer getAvailableReplicas() {
        return availableReplicas;
    }

    public List<String> getImages() {
        return images;
    }
}

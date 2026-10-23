package com.allst.boot.k8s.model;

public class PodView {

    private final String name;
    private final String namespace;
    private final String phase;
    private final String nodeName;
    private final String podIp;
    private final int readyContainers;
    private final int restartCount;
    private final String startTime;

    public PodView(String name, String namespace, String phase, String nodeName, String podIp,
                   int readyContainers, int restartCount, String startTime) {
        this.name = name;
        this.namespace = namespace;
        this.phase = phase;
        this.nodeName = nodeName;
        this.podIp = podIp;
        this.readyContainers = readyContainers;
        this.restartCount = restartCount;
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPhase() {
        return phase;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getPodIp() {
        return podIp;
    }

    public int getReadyContainers() {
        return readyContainers;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public String getStartTime() {
        return startTime;
    }
}

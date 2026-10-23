package com.allst.boot.k8s.model;

public class NamespaceView {

    private final String name;
    private final String phase;

    public NamespaceView(String name, String phase) {
        this.name = name;
        this.phase = phase;
    }

    public String getName() {
        return name;
    }

    public String getPhase() {
        return phase;
    }
}

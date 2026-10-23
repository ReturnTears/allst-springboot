package com.allst.boot.k8s.model;

import java.util.List;
import java.util.Map;

public class ConfigMapView {

    private final String name;
    private final String namespace;
    private final List<String> keys;
    private final Map<String, String> data;

    public ConfigMapView(String name, String namespace, List<String> keys, Map<String, String> data) {
        this.name = name;
        this.namespace = namespace;
        this.keys = keys;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<String> getKeys() {
        return keys;
    }

    public Map<String, String> getData() {
        return data;
    }
}

package com.allst.boot.web;

import java.util.Map;

public class ConfigMapRequest {

    private Map<String, String> data;

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}

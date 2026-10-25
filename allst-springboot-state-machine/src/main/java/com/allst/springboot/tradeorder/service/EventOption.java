package com.allst.springboot.tradeorder.service;

public class EventOption {

    private String code;
    private String label;

    public EventOption() {
    }

    public EventOption(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

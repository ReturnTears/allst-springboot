package com.allst.springboot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canal 行变更在 MQ 中的统一载体。三个下游只依赖这份结构，不依赖 Canal 协议对象。
 */
@Data
public class DataChangeEvent {

    public static final String INSERT = "INSERT";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";

    private String messageId;
    private String eventType;
    private String schema;
    private String table;
    private long executeTime;
    private String logfile;
    private long logfileOffset;
    private List<String> primaryKeys = new ArrayList<>();
    private String primaryValue;
    private Map<String, String> before = new LinkedHashMap<>();
    private Map<String, String> after = new LinkedHashMap<>();

    public boolean delete() {
        return DELETE.equals(eventType);
    }
}

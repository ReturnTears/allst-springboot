package com.allst.springboot.web;

import com.allst.springboot.canal.SyncProperties;
import com.allst.springboot.sync.ChangeDocument;
import com.allst.springboot.sync.es.EsSyncService;
import com.allst.springboot.sync.mongo.MongoSyncService;
import com.allst.springboot.sync.redis.RedisSyncService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/sync")
public class SyncInspectController {

    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9_.:-]+");

    private final SyncProperties syncProperties;
    private final ObjectProvider<RedisSyncService> redisSyncService;
    private final ObjectProvider<EsSyncService> esSyncService;
    private final ObjectProvider<MongoSyncService> mongoSyncService;

    public SyncInspectController(SyncProperties syncProperties,
                                 ObjectProvider<RedisSyncService> redisSyncService,
                                 ObjectProvider<EsSyncService> esSyncService,
                                 ObjectProvider<MongoSyncService> mongoSyncService) {
        this.syncProperties = syncProperties;
        this.redisSyncService = redisSyncService;
        this.esSyncService = esSyncService;
        this.mongoSyncService = mongoSyncService;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application", "allst-springboot-canal");
        body.put("redis", syncProperties.getRedis().isEnabled());
        body.put("elasticsearch", syncProperties.getElasticsearch().isEnabled());
        body.put("mongodb", syncProperties.getMongodb().isEnabled());
        return body;
    }

    @GetMapping
    public Map<String, Object> inspect(@RequestParam(defaultValue = "demo") String schema,
                                       @RequestParam String table,
                                       @RequestParam String id) {
        requireSafe("schema", schema);
        requireSafe("table", table);
        requireSafe("id", id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schema", schema);
        body.put("table", table);
        body.put("id", id);
        body.put("redis", redisView(schema, table, id));
        body.put("elasticsearch", elasticsearchView(schema, table, id));
        body.put("mongodb", mongodbView(schema, table, id));
        return body;
    }

    private Map<String, Object> redisView(String schema, String table, String id) {
        RedisSyncService service = redisSyncService.getIfAvailable();
        if (service == null) {
            return disabled();
        }
        return read("key", service.key(schema, table, id), () -> service.find(schema, table, id));
    }

    private Map<String, Object> elasticsearchView(String schema, String table, String id) {
        EsSyncService service = esSyncService.getIfAvailable();
        if (service == null) {
            return disabled();
        }
        return read("index", ChangeDocument.targetName(schema, table), () -> service.find(schema, table, id));
    }

    private Map<String, Object> mongodbView(String schema, String table, String id) {
        MongoSyncService service = mongoSyncService.getIfAvailable();
        if (service == null) {
            return disabled();
        }
        return read("collection", ChangeDocument.targetName(schema, table), () -> service.find(schema, table, id));
    }

    private Map<String, Object> read(String targetKey, String target, Supplier<Object> loader) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", true);
        body.put(targetKey, target);
        try {
            Object data = loader.get();
            body.put("found", data != null);
            body.put("data", data);
        } catch (RuntimeException ex) {
            body.put("found", false);
            body.put("error", ex.getMessage());
        }
        return body;
    }

    private Map<String, Object> disabled() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", false);
        return body;
    }

    private void requireSafe(String name, String value) {
        if (value == null || !SAFE.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " 只允许字母、数字、下划线、点、冒号和中划线");
        }
    }
}

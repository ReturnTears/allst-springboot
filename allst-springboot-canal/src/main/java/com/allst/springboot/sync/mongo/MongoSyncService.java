package com.allst.springboot.sync.mongo;

import com.allst.springboot.model.DataChangeEvent;
import com.allst.springboot.sync.BinlogCursor;
import com.allst.springboot.sync.ChangeDocument;
import com.allst.springboot.sync.SyncAccessException;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "sync.mongodb", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MongoSyncService {

    private final MongoTemplate mongoTemplate;

    public MongoSyncService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void apply(DataChangeEvent event) {
        if (event.getPrimaryValue() == null || event.getPrimaryValue().isEmpty()) {
            log.warn("MongoDB 跳过没有主键的变更, table={}", event.getTable());
            return;
        }
        String collection = ChangeDocument.targetName(event.getSchema(), event.getTable());
        String id = event.getPrimaryValue();
        try {
            Document current = mongoTemplate.findById(id, Document.class, collection);
            if (current != null && !BinlogCursor.shouldApply(current.getString(BinlogCursor.FILE),
                    ChangeDocument.toLong(current.get(BinlogCursor.OFFSET)), event.getLogfile(), event.getLogfileOffset())) {
                log.info("MongoDB 跳过更旧的变更, collection={}, id={}, incoming={}@{}", collection, id, event.getLogfile(), event.getLogfileOffset());
                return;
            }
            Query query = Query.query(Criteria.where("_id").is(id));
            if (event.delete()) {
                mongoTemplate.remove(query, collection);
                log.info("MongoDB 已删除 {}/{}", collection, id);
                return;
            }
            Document document = new Document(ChangeDocument.row(event));
            document.put("_id", id);
            mongoTemplate.save(document, collection);
            log.info("MongoDB 已写入 {}/{}", collection, id);
        } catch (RuntimeException ex) {
            throw new SyncAccessException("写入 MongoDB 失败: " + collection + "/" + id, ex);
        }
    }

    public Document find(String schema, String table, String id) {
        String collection = ChangeDocument.targetName(schema, table);
        try {
            return mongoTemplate.findById(id, Document.class, collection);
        } catch (RuntimeException ex) {
            throw new SyncAccessException("读取 MongoDB 失败: " + collection + "/" + id, ex);
        }
    }
}

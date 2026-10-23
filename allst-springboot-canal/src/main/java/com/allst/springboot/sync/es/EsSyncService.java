package com.allst.springboot.sync.es;

import com.allst.springboot.model.DataChangeEvent;
import com.allst.springboot.sync.BinlogCursor;
import com.allst.springboot.sync.ChangeDocument;
import com.allst.springboot.sync.SyncAccessException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "sync.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EsSyncService {

    private final RestHighLevelClient client;

    public EsSyncService(RestHighLevelClient client) {
        this.client = client;
    }

    public void apply(DataChangeEvent event) {
        if (event.getPrimaryValue() == null || event.getPrimaryValue().isEmpty()) {
            log.warn("Elasticsearch 跳过没有主键的变更, table={}", event.getTable());
            return;
        }
        String index = ChangeDocument.targetName(event.getSchema(), event.getTable());
        String id = event.getPrimaryValue();
        try {
            Map<String, Object> current = find(event.getSchema(), event.getTable(), id);
            if (current != null && !BinlogCursor.shouldApply(string(current.get(BinlogCursor.FILE)),
                    ChangeDocument.toLong(current.get(BinlogCursor.OFFSET)), event.getLogfile(), event.getLogfileOffset())) {
                log.info("Elasticsearch 跳过更旧的变更, index={}, id={}, incoming={}@{}", index, id, event.getLogfile(), event.getLogfileOffset());
                return;
            }
            if (event.delete()) {
                DeleteRequest request = new DeleteRequest(index, id);
                request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                client.delete(request, RequestOptions.DEFAULT);
                log.info("Elasticsearch 已删除 {}/{}", index, id);
                return;
            }
            IndexRequest request = new IndexRequest(index).id(id).source(ChangeDocument.row(event));
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client.index(request, RequestOptions.DEFAULT);
            log.info("Elasticsearch 已写入 {}/{}", index, id);
        } catch (ElasticsearchStatusException ex) {
            if (ex.status() == RestStatus.NOT_FOUND) {
                log.info("Elasticsearch 索引或文档不存在, index={}, id={}", index, id);
                return;
            }
            throw new SyncAccessException("写入 Elasticsearch 失败: " + index + "/" + id, ex);
        } catch (IOException | RuntimeException ex) {
            throw new SyncAccessException("写入 Elasticsearch 失败: " + index + "/" + id, ex);
        }
    }

    public Map<String, Object> find(String schema, String table, String id) {
        String index = ChangeDocument.targetName(schema, table);
        try {
            GetResponse response = client.get(new GetRequest(index, id), RequestOptions.DEFAULT);
            if (!response.isExists()) {
                return null;
            }
            return response.getSourceAsMap();
        } catch (ElasticsearchStatusException ex) {
            if (ex.status() == RestStatus.NOT_FOUND) {
                return null;
            }
            throw new SyncAccessException("读取 Elasticsearch 失败: " + index + "/" + id, ex);
        } catch (IOException ex) {
            throw new SyncAccessException("读取 Elasticsearch 失败: " + index + "/" + id, ex);
        }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

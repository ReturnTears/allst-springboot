package com.allst.springboot.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.allst.springboot.model.DataChangeEvent;
import com.allst.springboot.mq.DataChangePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 单线程拉取 Canal。只有整批消息都写入 RabbitMQ 之后才 ack；投递失败则 rollback，位点留在 Canal。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "canal.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CanalClientRunner implements SmartLifecycle {

    private final CanalProperties properties;
    private final CanalEntryParser parser;
    private final DataChangePublisher publisher;
    private final Object lifecycleMonitor = new Object();

    private volatile boolean running;
    private volatile CanalConnector connector;
    private Thread worker;

    public CanalClientRunner(CanalProperties properties, CanalEntryParser parser, DataChangePublisher publisher) {
        this.properties = properties;
        this.parser = parser;
        this.publisher = publisher;
    }

    @Override
    public void start() {
        synchronized (lifecycleMonitor) {
            if (running) {
                return;
            }
            running = true;
            worker = new Thread(this::loop, "canal-client");
            worker.setDaemon(true);
            worker.start();
        }
        log.info("Canal 客户端已启动, destination={}, subscribe={}", properties.getDestination(), properties.getSubscribe());
    }

    @Override
    public void stop() {
        Thread current;
        synchronized (lifecycleMonitor) {
            running = false;
            current = worker;
            closeQuietly(connector);
        }
        if (current != null) {
            current.interrupt();
            try {
                current.join(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Canal 客户端已停止");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void loop() {
        while (running) {
            CanalConnector current = null;
            try {
                current = open();
                this.connector = current;
                pull(current);
            } catch (Exception ex) {
                if (!running) {
                    break;
                }
                log.error("Canal 客户端异常，{} ms 后重连", properties.getRetryIntervalMs(), ex);
                sleep(properties.getRetryIntervalMs());
            } finally {
                closeQuietly(current);
                this.connector = null;
            }
        }
    }

    private CanalConnector open() {
        CanalConnector created = CanalConnectors.newSingleConnector(
                new InetSocketAddress(properties.getHost(), properties.getPort()),
                properties.getDestination(),
                properties.getUsername(),
                properties.getPassword());
        created.connect();
        created.subscribe(properties.getSubscribe());
        // 从未确认的位点继续，避免进程重启后丢增量
        created.rollback();
        log.info("已连接 Canal {}:{}, destination={}", properties.getHost(), properties.getPort(), properties.getDestination());
        return created;
    }

    private void pull(CanalConnector current) {
        while (running) {
            Message message = current.getWithoutAck(properties.getBatchSize(), 1L, TimeUnit.SECONDS);
            long batchId = message.getId();
            List<CanalEntry.Entry> entries = message.getEntries();
            if (batchId < 0 || entries == null || entries.isEmpty()) {
                continue;
            }
            try {
                List<DataChangeEvent> events = parser.parse(entries);
                publisher.publish(events);
                current.ack(batchId);
                if (!events.isEmpty()) {
                    log.info("Canal 批次 {} 已确认，投递 {} 条变更", batchId, events.size());
                }
            } catch (Exception ex) {
                log.error("Canal 批次 {} 投递失败，回滚后重试", batchId, ex);
                current.rollback();
                throw ex;
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private void closeQuietly(CanalConnector current) {
        if (current == null) {
            return;
        }
        try {
            current.disconnect();
        } catch (Exception ex) {
            log.debug("断开 Canal 连接时出现异常", ex);
        }
    }
}

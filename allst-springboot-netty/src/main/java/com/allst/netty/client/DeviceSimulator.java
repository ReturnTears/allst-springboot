package com.allst.netty.client;

import com.allst.netty.protocol.WarehouseFrameEncoder;
import com.allst.netty.protocol.WarehouseMessage;
import com.allst.netty.protocol.WarehouseMessageDecoder;
import com.allst.netty.session.DeviceOfflineException;
import com.allst.netty.tcp.NettyThreads;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在进程内拉起若干台虚拟扫码枪。phase 高于 TCP 服务，避免设备比端口更早启动。
 */
public final class DeviceSimulator implements SmartLifecycle {

    public static final int PHASE = 100;

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);

    private final NettyClientProperties properties;
    private final WarehouseFrameEncoder encoder;
    private final WarehouseMessageDecoder decoder;
    private final Map<String, WarehouseDeviceClient> clients = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private NioEventLoopGroup group;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scanTask;

    public DeviceSimulator(NettyClientProperties properties,
                           WarehouseFrameEncoder encoder,
                           WarehouseMessageDecoder decoder) {
        this.properties = properties;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public static String deviceId(int index) {
        return String.format("WH-SCANNER-%02d", index);
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        this.group = new NioEventLoopGroup(properties.getWorkerThreads(), new DefaultThreadFactory("device-client"));
        for (int index = 1; index <= properties.getDeviceCount(); index++) {
            String deviceId = deviceId(index);
            WarehouseDeviceClient client = new WarehouseDeviceClient(
                    properties.getHost(),
                    properties.getPort(),
                    deviceId,
                    properties.getHeartbeatSeconds(),
                    properties.getReconnectSeconds(),
                    properties.getMaxFrameLength(),
                    group,
                    false,
                    encoder,
                    decoder,
                    null
            );
            clients.put(deviceId, client);
            client.start();
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "scanner-sim");
            thread.setDaemon(true);
            return thread;
        });
        long initialDelay = Math.min(3, properties.getScanIntervalSeconds());
        this.scanTask = scheduler.scheduleAtFixedRate(
                this::emitRandomScan,
                initialDelay,
                properties.getScanIntervalSeconds(),
                TimeUnit.SECONDS
        );
        log.info("模拟扫码枪已启动，共 {} 台，目标 {}:{}", clients.size(), properties.getHost(), properties.getPort());
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (scanTask != null) {
            scanTask.cancel(false);
        }
        for (WarehouseDeviceClient client : clients.values()) {
            client.stop();
        }
        NettyThreads.shutdown(group);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        log.info("模拟扫码枪已停止");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    public void publishScan(String deviceId, String barcode, String location, int quantity) {
        WarehouseDeviceClient client = clients.get(deviceId);
        if (client == null || !client.isActive()) {
            throw new DeviceOfflineException(deviceId);
        }
        client.send(WarehouseMessage.scan(deviceId, barcode, location, quantity));
    }

    private void emitRandomScan() {
        if (!running.get()) {
            return;
        }
        WarehouseDeviceClient[] active = clients.values().stream()
                .filter(WarehouseDeviceClient::isActive)
                .toArray(WarehouseDeviceClient[]::new);
        if (active.length == 0) {
            return;
        }
        WarehouseDeviceClient client = active[ThreadLocalRandom.current().nextInt(active.length)];
        String barcode = "SKU-" + (10000 + ThreadLocalRandom.current().nextInt(90000));
        int aisle = 1 + ThreadLocalRandom.current().nextInt(5);
        int bin = 1 + ThreadLocalRandom.current().nextInt(4);
        String location = "A-0" + aisle + "-0" + bin;
        try {
            client.send(WarehouseMessage.scan(client.deviceId(), barcode, location, 1));
        } catch (RuntimeException ex) {
            log.debug("模拟扫码跳过 deviceId={}", client.deviceId(), ex);
        }
    }
}

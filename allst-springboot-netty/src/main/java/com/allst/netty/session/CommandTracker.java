package com.allst.netty.session;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 指令回执表。只保留最近 1000 条，避免演示进程把内存慢慢吃满。
 */
public final class CommandTracker {

    private static final int MAX_RECORDS = 1000;

    private final ConcurrentMap<String, CommandRecord> records = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> order = new ConcurrentLinkedDeque<>();

    public CommandRecord track(String requestId, String deviceId, String action, java.util.Map<String, Object> params) {
        CommandRecord record = new CommandRecord(
                requestId,
                deviceId,
                action,
                params,
                CommandStatus.SENT,
                System.currentTimeMillis(),
                null
        );
        records.put(requestId, record);
        order.addLast(requestId);
        trim();
        return record;
    }

    public Optional<CommandRecord> find(String requestId) {
        return Optional.ofNullable(records.get(requestId));
    }

    public Optional<CommandRecord> markAcked(String requestId) {
        return update(requestId, current -> {
            if (current.status() == CommandStatus.ACKED) {
                return current;
            }
            return current.acked(System.currentTimeMillis());
        });
    }

    public Optional<CommandRecord> markFailed(String requestId) {
        return update(requestId, current -> {
            if (current.status() != CommandStatus.SENT) {
                return current;
            }
            return current.failed();
        });
    }

    private Optional<CommandRecord> update(String requestId, java.util.function.UnaryOperator<CommandRecord> change) {
        AtomicReference<CommandRecord> updated = new AtomicReference<>();
        records.computeIfPresent(requestId, (id, current) -> {
            CommandRecord next = change.apply(current);
            updated.set(next);
            return next;
        });
        return Optional.ofNullable(updated.get());
    }

    private void trim() {
        while (order.size() > MAX_RECORDS) {
            String expired = order.pollFirst();
            if (expired != null) {
                records.remove(expired);
            }
        }
    }
}

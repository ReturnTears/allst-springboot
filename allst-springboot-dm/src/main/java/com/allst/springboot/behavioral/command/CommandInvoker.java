package com.allst.springboot.behavioral.command;

import com.allst.springboot.support.KeyValueStore;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 命令执行器：幂等去重 + 有限重试 + 撤销栈 + 执行流水。
 * <Pre>
 * 幂等用 KeyValueStore.putIfAbsent（SETNX 语义）：默认内存实现，
 * 打开 dm.redis.enabled 后换成 Redis，同一个命令在多实例部署下也只执行一次。
 * 执行失败会释放幂等键，允许下一次重试真正重跑；这点在生产里很容易写错。
 */
@Component
public class CommandInvoker {

    private static final long IDEMPOTENT_TTL_SECONDS = 300;

    private final KeyValueStore store;
    private final Deque<OrderCommand> undoStack = new ArrayDeque<>();
    private final List<String> history = new CopyOnWriteArrayList<>();

    public CommandInvoker(KeyValueStore store) {
        this.store = store;
    }

    public CommandResult run(OrderCommand command, Map<String, Object> context) {
        String key = "dm:cmd:" + command.id();
        if (!store.putIfAbsent(key, "DONE", IDEMPOTENT_TTL_SECONDS)) {
            history.add("DUPLICATE " + command.id());
            return CommandResult.duplicate(command.id());
        }

        int attempts = 0;
        RuntimeException lastError = null;
        int maxAttempts = Math.max(1, command.maxAttempts());
        while (attempts < maxAttempts) {
            attempts++;
            try {
                String result = command.execute(context);
                synchronized (undoStack) {
                    undoStack.push(command);
                }
                history.add("OK " + command.id() + " attempts=" + attempts);
                return CommandResult.ok(command.id(), result, attempts);
            } catch (RuntimeException e) {
                lastError = e;
                history.add("RETRY " + command.id() + " attempts=" + attempts + " cause=" + e.getMessage());
            }
        }
        // 彻底失败：释放幂等键，允许上游稍后重试同一个命令
        store.delete(key);
        return CommandResult.failed(command.id(), attempts,
                lastError == null ? "未知错误" : lastError.getMessage());
    }

    public CommandResult undoLast(Map<String, Object> context) {
        OrderCommand command;
        synchronized (undoStack) {
            command = undoStack.poll();
        }
        if (command == null) {
            return CommandResult.failed("none", 0, "没有可撤销的命令");
        }
        command.undo(context);
        store.delete("dm:cmd:" + command.id());
        history.add("UNDO " + command.id());
        return CommandResult.ok(command.id(), "已撤销 " + command.id(), 1);
    }

    public List<String> history() {
        return List.copyOf(history);
    }

    @Getter
    public static final class CommandResult {
        private final String commandId;
        private final String status;
        private final String result;
        private final int attempts;

        private CommandResult(String commandId, String status, String result, int attempts) {
            this.commandId = commandId;
            this.status = status;
            this.result = result;
            this.attempts = attempts;
        }

        static CommandResult ok(String id, String result, int attempts) {
            return new CommandResult(id, "OK", result, attempts);
        }

        static CommandResult duplicate(String id) {
            return new CommandResult(id, "DUPLICATE", "命令已执行过，直接返回上次结果", 0);
        }

        static CommandResult failed(String id, int attempts, String reason) {
            return new CommandResult(id, "FAILED", reason, attempts);
        }
    }
}

package com.allst.boot.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 函数回调模板：<b>把「分批、校验、转换、落库、汇总」的骨架写一次，业务用 lambda 填空</b>。
 * <p>
 * 这是「在回调里用 Stream」最典型的形态，也是通用框架剥离业务定制逻辑的标准做法。
 * 对比传统写法：每个导入功能都要重复写一遍分批循环、try-catch、成功失败计数，
 * 骨架和业务搅在一起，改一处要改十个地方。
 * <p>
 * 四大接口在这里各司其职：
 * <ul>
 *   <li>{@code Supplier} —— 数据从哪来（延迟到真正要处理时才拉取）</li>
 *   <li>{@code Predicate} —— 哪些数据合格</li>
 *   <li>{@code Function} —— 怎么转换成目标对象</li>
 *   <li>{@code Consumer} —— 落库/发消息等副作用</li>
 * </ul>
 */
@Slf4j
@Component
public class BatchTemplate {

    /** 批处理结果：成功数、失败数、被过滤数、失败原因。 */
    public static class BatchResult {
        private final int succeeded;
        private final int failed;
        private final int skipped;
        private final List<String> errors;

        BatchResult(int succeeded, int failed, int skipped, List<String> errors) {
            this.succeeded = succeeded;
            this.failed = failed;
            this.skipped = skipped;
            this.errors = List.copyOf(errors);
        }

        public int getSucceeded() {
            return succeeded;
        }

        public int getFailed() {
            return failed;
        }

        public int getSkipped() {
            return skipped;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getTotal() {
            return succeeded + failed + skipped;
        }
    }

    /**
     * 通用批处理：拉取 → 过滤 → 转换 → 分批消费 → 汇总。
     * <p>
     * 单条失败不影响整批（这是数据同步的刚需），失败原因收集起来回传。
     * 分批用 {@link #partition} 而不是手写 subList 边界判断。
     *
     * @param source    数据来源，延迟执行
     * @param accept    合格判断，不合格的计入 skipped
     * @param converter 转换逻辑，抛异常计入 failed
     * @param saver     批量落库，整批失败则该批全部计入 failed
     * @param batchSize 每批条数
     */
    public <S, T> BatchResult process(Supplier<List<S>> source,
                                      Predicate<S> accept,
                                      Function<S, T> converter,
                                      Consumer<List<T>> saver,
                                      int batchSize) {
        List<S> raw = source.get();
        if (raw == null || raw.isEmpty()) {
            return new BatchResult(0, 0, 0, List.of());
        }

        List<S> accepted = raw.stream().filter(accept).collect(Collectors.toList());
        int skipped = raw.size() - accepted.size();

        List<String> errors = new ArrayList<>();
        List<T> converted = new ArrayList<>();
        for (S item : accepted) {
            try {
                converted.add(converter.apply(item));
            } catch (RuntimeException ex) {
                errors.add(String.valueOf(item) + ": " + ex.getMessage());
            }
        }

        int[] succeeded = {0};
        for (List<T> batch : partition(converted, batchSize)) {
            try {
                saver.accept(batch);
                succeeded[0] += batch.size();
            } catch (RuntimeException ex) {
                errors.add("batch(" + batch.size() + "): " + ex.getMessage());
            }
        }
        int failed = converted.size() - succeeded[0] + (accepted.size() - converted.size());
        return new BatchResult(succeeded[0], failed, skipped, errors);
    }

    /**
     * 分批：用 {@code IntStream.iterate} 生成起点，再 mapToObj 取 subList。
     * <p>
     * 比 {@code for (int i = 0; i < size; i += batchSize)} 更能表达意图，
     * 且天然处理了「最后一批不足 batchSize」的边界（Math.min 收口）。
     * <p>
     * 业务场景：批量插入避免单条 SQL 太慢，也避免一次 insert 几万条撑爆参数上限。
     */
    public <T> List<List<T>> partition(List<T> items, int batchSize) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须为正数");
        }
        int total = items.size();
        return IntStream.iterate(0, start -> start < total, start -> start + batchSize)
                .mapToObj(start -> items.subList(start, Math.min(start + batchSize, total)))
                .collect(Collectors.toList());
    }

    /**
     * 重试模板：把「可能失败的操作」包成 Supplier，失败按次数重试。
     * <p>
     * 用 {@code IntStream.range + mapToObj + filter + findFirst} 表达「试到成功为止」，
     * 短路特性保证成功后不再继续尝试。业务场景：调第三方接口偶发超时。
     */
    public <T> java.util.Optional<T> retry(Supplier<T> action, int maxAttempts) {
        return IntStream.rangeClosed(1, maxAttempts)
                .mapToObj(attempt -> {
                    try {
                        return action.get();
                    } catch (RuntimeException ex) {
                        log.warn("第 {} 次尝试失败: {}", attempt, ex.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }
}
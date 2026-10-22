package com.allst.springboot.stress;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地压测入口，不会随应用启动，也不会被单元测试调用。
 * 默认打健康检查，避免把签名、落库和限流当成接口本身的耗时。
 *
 * <pre>
 * mvn -DskipTests compile exec:java -Dexec.mainClass=com.allst.springboot.stress.ApiStressMain -Dexec.args="http://127.0.0.1:1234/api/health 20 200"
 * </pre>
 *
 * 对应规范：11 压测。
 */
public final class ApiStressMain {

    private ApiStressMain() {
    }

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://127.0.0.1:1234/api/health";
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int total = args.length > 2 ? Integer.parseInt(args[2]) : 200;
        if (threads < 1 || total < 1) {
            throw new IllegalArgumentException("线程数和请求数必须大于 0");
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(total);
        ConcurrentLinkedQueue<Long> costs = new ConcurrentLinkedQueue<>();
        AtomicInteger errors = new AtomicInteger();
        long wallStart = System.nanoTime();
        for (int i = 0; i < total; i++) {
            pool.submit(() -> {
                try {
                    long begin = System.nanoTime();
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();
                    HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                    long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
                    if (response.statusCode() == 200) {
                        costs.add(costMs);
                    } else {
                        errors.incrementAndGet();
                    }
                } catch (Exception ex) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        done.await();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - wallStart);
        pool.shutdown();
        List<Long> sorted = new ArrayList<>(costs);
        Collections.sort(sorted);
        System.out.println("url=" + url);
        System.out.println("total=" + total + " threads=" + threads + " elapsedMs=" + elapsedMs);
        System.out.println("success=" + sorted.size() + " errors=" + errors.get());
        if (!sorted.isEmpty() && elapsedMs > 0) {
            System.out.printf("qps=%.2f%n", sorted.size() * 1000.0 / elapsedMs);
            System.out.println("p50=" + percentile(sorted, 50) + "ms");
            System.out.println("p95=" + percentile(sorted, 95) + "ms");
            System.out.println("p99=" + percentile(sorted, 99) + "ms");
            System.out.println("max=" + sorted.get(sorted.size() - 1) + "ms");
        }
    }

    private static long percentile(List<Long> sorted, int percent) {
        int index = (int) Math.ceil(percent / 100.0 * sorted.size()) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= sorted.size()) {
            index = sorted.size() - 1;
        }
        return sorted.get(index);
    }
}

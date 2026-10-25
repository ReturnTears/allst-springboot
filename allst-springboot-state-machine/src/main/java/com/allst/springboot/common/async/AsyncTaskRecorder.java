package com.allst.springboot.common.async;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 教学用的异步任务回执。生产环境的审计以数据库流水为准，这里只方便观察线程池是否执行过。
 */
@Component
public class AsyncTaskRecorder {

    private final ConcurrentLinkedQueue<String> done = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> failed = new ConcurrentLinkedQueue<>();

    public void markDone(String taskName) {
        done.add(taskName);
    }

    public void markFailed(String taskName) {
        failed.add(taskName);
    }

    public int countDone(String taskName) {
        return count(done, taskName);
    }

    public List<String> snapshot() {
        List<String> all = new ArrayList<>();
        done.forEach(name -> all.add("done:" + name));
        failed.forEach(name -> all.add("failed:" + name));
        return all;
    }

    private int count(ConcurrentLinkedQueue<String> queue, String taskName) {
        int count = 0;
        for (String item : queue) {
            if (taskName.equals(item)) {
                count++;
            }
        }
        return count;
    }
}

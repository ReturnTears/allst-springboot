package com.allst.netty.session;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 最近扫码的内存环形日志。容量固定，新事件排在前面。
 */
public final class RecentEventLog {

    private final int capacity;
    private final Deque<ScanEvent> events = new ArrayDeque<>();

    public RecentEventLog(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized void append(ScanEvent event) {
        events.addFirst(event);
        while (events.size() > capacity) {
            events.removeLast();
        }
    }

    public synchronized List<ScanEvent> latest(int limit) {
        int size = Math.max(0, Math.min(limit, events.size()));
        List<ScanEvent> copy = new ArrayList<>(size);
        int index = 0;
        for (ScanEvent event : events) {
            if (index++ >= size) {
                break;
            }
            copy.add(event);
        }
        return List.copyOf(copy);
    }
}

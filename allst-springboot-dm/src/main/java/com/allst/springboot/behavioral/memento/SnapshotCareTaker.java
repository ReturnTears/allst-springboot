package com.allst.springboot.behavioral.memento;

import com.allst.springboot.behavioral.memento.CartSnapshot.Memento;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 负责人：只保管快照，不解释快照内容。示例用单例栈；生产里应按业务单据隔离历史，
 * 否则不同订单的撤销会互相串。
 */
@Component
public class SnapshotCareTaker {

    private final Deque<Memento> history = new ArrayDeque<>();

    public void push(Memento memento) {
        synchronized (history) {
            history.push(memento);
        }
    }

    public Memento undo() {
        synchronized (history) {
            return history.poll();
        }
    }

    public List<String> labels() {
        synchronized (history) {
            List<String> labels = new ArrayList<>();
            history.forEach(m -> labels.add(m.getLabel()));
            return labels;
        }
    }

    public int size() {
        synchronized (history) {
            return history.size();
        }
    }
}

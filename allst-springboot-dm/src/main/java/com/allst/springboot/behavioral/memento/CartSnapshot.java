package com.allst.springboot.behavioral.memento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 备忘录（Memento）——购物车/订单在改价、改数量之前存一份快照，用于撤销和留痕。
 *
 * 场景：客服在后台帮客户改单，改错了要能一键回到改前；风控也要看「改了什么」。
 * 直接暴露对象内部字段让外部保存，会把数据结构泄漏出去；备忘录让原发器自己
 * 产生/消费快照，负责人（CareTaker）只负责保管，不读里面的内容。
 *
 * 快照必须是不可变副本，否则「改前」会跟着对象一起变，撤销就失效了。
 * 对应的成熟做法：工作流表单快照、Seata 的 undo log、编辑器的 undo 栈。
 */
public final class CartSnapshot {

    private CartSnapshot() {
    }

    @lombok.Getter
    public static final class Memento {
        private final List<Line> lines;
        private final BigDecimal total;
        private final String label;

        private Memento(List<Line> lines, BigDecimal total, String label) {
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.total = total;
            this.label = label;
        }
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static final class Line {
        private final String sku;
        private final int quantity;
        private final BigDecimal price;

        public BigDecimal subtotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public static final class CartEditor {
        private final List<Line> lines = new ArrayList<>();

        public void add(String sku, int quantity, BigDecimal price) {
            lines.add(new Line(sku, quantity, price));
        }

        public void clear() {
            lines.clear();
        }

        public BigDecimal total() {
            return lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public List<String> view() {
            List<String> view = new ArrayList<>();
            lines.forEach(l -> view.add(l.getSku() + " x" + l.getQuantity()));
            return view;
        }

        public Memento save(String label) {
            return new Memento(lines, total(), label);
        }

        public void restore(Memento memento) {
            lines.clear();
            lines.addAll(memento.getLines());
        }
    }
}

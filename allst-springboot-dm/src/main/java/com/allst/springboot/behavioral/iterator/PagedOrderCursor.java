package com.allst.springboot.behavioral.iterator;

import com.allst.springboot.domain.OrderRecord;
import com.allst.springboot.domain.OrderRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 迭代器（Iterator）——把「分页查库」藏在一个 Iterator 后面。
 * <Pre>
 * 场景：对账、导出、批量补发券要遍历几十万笔订单，一次性 findAll() 会打爆内存，
 * 而每个批处理任务又都要复制一遍「page++ / hasNext / 边界」的分页样板代码。
 * 迭代器让调用方写 while(cursor.hasNext())，按批拉数据，内存里始终只有一批。
 * <Pre>
 * 对应的成熟实现：MyBatis Cursor、Spring Batch 的 ItemReader、JPA 的 Stream 结果。
 * 注意分页要固定排序字段，否则翻页时可能出现重复或漏行。
 */
public class PagedOrderCursor implements Iterator<OrderRecord> {

    private final OrderRecordRepository repository;
    private final int batchSize;
    private Iterator<OrderRecord> current = Collections.emptyIterator();
    private int page = 0;
    private int batchesRead = 0;
    private boolean exhausted = false;

    public PagedOrderCursor(OrderRecordRepository repository, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize 至少为 1");
        }
        this.repository = repository;
        this.batchSize = batchSize;
    }

    @Override
    public boolean hasNext() {
        if (!current.hasNext() && !exhausted) {
            fetch();
        }
        return current.hasNext();
    }

    @Override
    public OrderRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return current.next();
    }

    public int batchesRead() {
        return batchesRead;
    }

    private void fetch() {
        Page<OrderRecord> batch = repository.findAll(
                PageRequest.of(page, batchSize, Sort.by(Sort.Direction.ASC, "id")));
        page++;
        batchesRead++;
        current = batch.getContent().iterator();
        exhausted = !batch.hasNext();
    }
}

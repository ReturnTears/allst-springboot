package com.allst.boot.dao;

import com.allst.boot.domain.Book;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Hutu
 * @since 2024-08-14 下午 11:05
 */
@Repository
public class BookDaoRepository {
    private final ConcurrentMap<Long, Book> repository = new ConcurrentHashMap<>();
    private static final AtomicLong idGenerator = new AtomicLong(0);

    public Long save(Book book) {
        Long id = idGenerator.incrementAndGet();
        book.setId(id);
        repository.put(id, book);
        return id;
    }

    public Collection<Book> findAll() {
        return repository.values();
    }

    public Book findBookById(Long id) {
        return repository.get(id);
    }

    public Long updateBook(Book book) {
        repository.put(book.getId(), book);
        return book.getId();
    }

    public Long deleteBook(Long id) {
        repository.remove(id);
        return id;
    }
}

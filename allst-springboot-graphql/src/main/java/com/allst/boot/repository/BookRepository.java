package com.allst.boot.repository;

import com.allst.boot.model.Book;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Hutu
 * @since 2024-07-29 下午 09:19
 */
public interface BookRepository extends CrudRepository<Book, Integer> {
    Book findByName(String name);
}

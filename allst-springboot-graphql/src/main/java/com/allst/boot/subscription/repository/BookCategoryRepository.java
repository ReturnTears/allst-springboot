package com.allst.boot.subscription.repository;

import com.allst.boot.subscription.model.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Integer> {
    Optional<BookCategory> findByBookId(Integer bookId);

    List<BookCategory> findByBookIdIn(Collection<Integer> bookIds);

    List<BookCategory> findByCategoryIdIn(Collection<Integer> categoryIds);

    int countByCategoryId(Integer categoryId);

    void deleteByBookId(Integer bookId);
}

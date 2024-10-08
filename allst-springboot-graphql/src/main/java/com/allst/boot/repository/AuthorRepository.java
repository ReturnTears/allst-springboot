package com.allst.boot.repository;

import com.allst.boot.model.Author;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Hutu
 * @since 2024-07-29 下午 09:19
 */
public interface AuthorRepository extends CrudRepository<Author, Integer> {
    Author findAuthorByBookId(Integer bookId);

    @Modifying
    @Query("update Author m set m.firstName=?2 where m.id=?1")
    void updateAuthor(Integer id, String firstName);
}

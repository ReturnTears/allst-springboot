package com.allst.boot.queryresolvers;

import com.allst.boot.model.Author;
import com.allst.boot.model.Book;
import com.allst.boot.repository.AuthorRepository;
import com.allst.boot.repository.BookRepository;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Hutu
 * @since 2024-07-29 下午 09:20
 */
@Component
public class BookQuery implements GraphQLQueryResolver {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    public Iterable<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book getBookByName(String name) {
        return bookRepository.findBookByName(name);
    }

    public Iterable<Author> allAuthor() {
        return authorRepository.findAll();
    }
}

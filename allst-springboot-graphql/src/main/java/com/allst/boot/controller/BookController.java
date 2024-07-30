package com.allst.boot.controller;

import com.allst.boot.model.Author;
import com.allst.boot.model.Book;
import com.allst.boot.queryresolvers.BookQuery;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Hutu
 * @since 2024-07-29 下午 09:24
 */
@RestController
@RequestMapping("/book")
public class BookController {
    @Autowired
    private BookQuery bookQuery;

    @GetMapping("/getAll")
    public List<Book> getBook() {
        Iterable<Book> books = bookQuery.findAll();
        List<Book> result = Lists.newArrayList();
        for (Book book : books) {
            result.add(book);
        }
        return result;
    }

    @GetMapping("/getByName")
    public Book getBookByName(String name) {
        return bookQuery.getBookByName(name);
    }

    @GetMapping("/getByAuthor")
    public List<Author> getBookByAuthor() {
        Iterable<Author> authors = bookQuery.allAuthor();
        List<Author> result = Lists.newArrayList();
        for (Author author : authors) {
            result.add(author);
        }
        return result;
    }
}

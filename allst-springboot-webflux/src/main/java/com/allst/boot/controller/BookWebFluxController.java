package com.allst.boot.controller;

import com.allst.boot.domain.Book;
import com.allst.boot.handler.BookHandler;
import com.allst.boot.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Hutu
 * @since 2024-08-14 下午 11:14
 */
@RestController
@RequestMapping(value = "/book")
public class BookWebFluxController {
    private final BookHandler bookHandler;

    public BookWebFluxController(BookHandler bookHandler) {
        this.bookHandler = bookHandler;
    }

    @Autowired
    private BookRepository bookRepository;

    @GetMapping(value = "/findAll")
    public Flux<Book> findAll() {
        return bookHandler.findAllBook();
    }

    @GetMapping(value = "/findBy/{id}")
    public Mono<Book> findById(@PathVariable("id") Long id) {
        Book book = bookRepository.findBookById(id);
        System.out.println("book : " + book);
        return Mono.just(book);
    }

    @PostMapping(value = "/save")
    public Mono<Long> save(Book book) {
        return bookHandler.save(book);
    }

    @PutMapping()
    public Mono<Long> modifyCity(@RequestBody Book book) {
        return bookHandler.modifyBook(book);
    }

    @DeleteMapping(value = "/{id}")
    public Mono<Long> deleteCity(@PathVariable("id") Long id) {
        return bookHandler.deleteBook(id);
    }
}

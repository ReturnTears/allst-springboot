package com.allst.boot.handler;

import com.allst.boot.dao.BookDaoRepository;
import com.allst.boot.domain.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Hutu
 * @since 2024-08-14 下午 11:09
 */
@Component
public class BookHandler {
    private final BookDaoRepository bookRepository;

    @Autowired
    public BookHandler(BookDaoRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Mono<Long> save(Book book) {
        return Mono.create(bookMonoSink -> bookMonoSink.success(bookRepository.save(book)));
    }

    public Mono<Book> findBookById(Long id) {
        return Mono.justOrEmpty(bookRepository.findBookById(id));
    }

    public Flux<Book> findAllBook() {
        return Flux.fromIterable(bookRepository.findAll());
    }

    public Mono<Long> modifyBook(Book city) {
        return Mono.create(cityMonoSink -> cityMonoSink.success(bookRepository.updateBook(city)));
    }

    public Mono<Long> deleteBook(Long id) {
        return Mono.create(cityMonoSink -> cityMonoSink.success(bookRepository.deleteBook(id)));
    }
}

package com.allst.boot.controller;

import com.allst.boot.domain.Book;
import com.allst.boot.handler.BookHandler;
import com.allst.boot.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Hutu
 * @since 2024-08-14 下午 11:14
 */
@Controller
@RequestMapping(value = "/book")
public class BookWebFluxWebController {
    private final BookHandler bookHandler;
    private final BookRepository bookRepository;

    @Autowired
    public BookWebFluxWebController(BookHandler bookHandler, BookRepository bookRepository) {
        this.bookHandler = bookHandler;
        this.bookRepository = bookRepository;
    }

    @GetMapping("/hello")
    public Mono<String> hello(final Model model) {
        model.addAttribute("name", "KangKang");
        model.addAttribute("city", "hubei");
        String path = "hello";
        return Mono.create(monoSink -> monoSink.success(path));
    }

    @GetMapping("/page/list")
    public String listPage(final Model model) {
        Iterable<Book> all = bookRepository.findAll();
        final Flux<Book> cityFluxList = Flux.fromIterable(all);
        model.addAttribute("bookList", cityFluxList);
        return "bookList";
    }
}

package com.allst.boot.controller;

import com.allst.boot.domain.Book;
import com.allst.boot.domain.Lagou;
import com.allst.boot.handler.BookHandler;
import com.allst.boot.repository.BookRepository;
import com.allst.boot.repository.LagouRepository;
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
@RequestMapping(value = "/web")
public class BookWebFluxWebController {
    private final LagouRepository lagouRepository;
    private final BookRepository bookRepository;

    @Autowired
    public BookWebFluxWebController(LagouRepository lagouRepository, BookRepository bookRepository) {
        this.lagouRepository = lagouRepository;
        this.bookRepository = bookRepository;
    }

    @GetMapping("/hello")
    public Mono<String> hello(final Model model) {
        model.addAttribute("name", "KangKang");
        model.addAttribute("city", "hubei");
        String path = "hello";
        return Mono.create(x -> x.success(path));
    }

    @GetMapping("/websocket")
    public Mono<String> websocket(final Model model) {
        model.addAttribute("wbUrl", "ws://127.0.0.1:1314/test/echo");
        return Mono.create(x -> x.success("websocket"));
    }

    @GetMapping("/page/list")
    public String bookPage(final Model model) {
        Iterable<Book> all = bookRepository.findAll();
        final Flux<Book> bookFluxList = Flux.fromIterable(all);
        model.addAttribute("bookList", bookFluxList);
        return "bookList";
    }

    @GetMapping("/lagou/list")
    public String lagouPage(final Model model) {
        final Flux<Lagou> lagouList = lagouRepository.findAll();
        model.addAttribute("lagouList", lagouList);
        return "lagouList";
    }
}

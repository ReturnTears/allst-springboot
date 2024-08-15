package com.allst.boot.controller;

import com.allst.boot.domain.Lagou;
import com.allst.boot.repository.LagouRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author Hutu
 * @since 2024-08-15 下午 11:08
 */
@RestController
@RequestMapping(value = "/lagou")
public class LagouReactiveController {
    private final LagouRepository lagouRepository;

    @Autowired
    public LagouReactiveController(LagouRepository lagouRepository) {
        this.lagouRepository = lagouRepository;
    }

    @GetMapping(value = "/findByNameAndSalary")
    public Flux<Lagou> findByNameAndSalary(String name, Double salary) {
        return lagouRepository.findByNameAndSalary(name, salary);
    }

    @GetMapping(value = "/findByNameEquals")
    public Flux<Lagou> findByNameEquals(String name) {
        return lagouRepository.findByNameEquals(name);
    }

    @GetMapping(value = "/findAll")
    public Flux<Lagou> findAll() {
        return lagouRepository.findAll();
    }
}

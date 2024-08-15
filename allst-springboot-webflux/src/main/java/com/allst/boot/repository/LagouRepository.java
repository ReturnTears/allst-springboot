package com.allst.boot.repository;

import com.allst.boot.domain.Lagou;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author June
 * @since 2021年10月
 */
@Repository
public interface LagouRepository extends ReactiveMongoRepository<Lagou, String> {
    Flux<Lagou> findByNameEquals(String name);
    Flux<Lagou> findByNameAndSalary(String name, Double salary);
}

package com.allst.boot.subscription.repository;

import com.allst.boot.subscription.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);
}

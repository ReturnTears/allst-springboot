package com.allst.boot.subscription.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "book_category", schema = "demo")
public class BookCategory {
    @Id
    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;
}

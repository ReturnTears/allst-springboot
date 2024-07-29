package com.allst.boot.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "book", schema = "demo")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String name;
    @Column(name = "pageCount")
    private String pageCount;

    public Book(Integer id, String name, String pageCount) {
        this.id = id;
        this.name = name;
        this.pageCount = pageCount;
    }

    public Book() {
    }
}
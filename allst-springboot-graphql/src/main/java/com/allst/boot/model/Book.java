package com.allst.boot.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "book", schema = "demo")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;
    @Column(name = "name")
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
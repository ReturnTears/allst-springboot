package com.allst.boot.domain;

import lombok.Data;

import javax.persistence.*;


@Entity
@Data
@Table(name = "book", schema = "demo")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String pageCount;
}

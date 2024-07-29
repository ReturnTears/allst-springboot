package com.allst.boot.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "author", schema = "demo")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;
    @Column(name = "firstName")
    String firstName;
    @Column(name = "lastName")
    String lastName;
    @Column(name = "bookId")
    Integer bookId;

    public Author(Integer id, String firstName, String lastName, Integer bookId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bookId = bookId;
    }

    public Author() {

    }
}

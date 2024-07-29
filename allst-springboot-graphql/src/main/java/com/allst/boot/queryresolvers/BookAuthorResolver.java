package com.allst.boot.queryresolvers;

import com.allst.boot.model.Author;
import com.allst.boot.model.Book;
import com.allst.boot.repository.AuthorRepository;
import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookAuthorResolver implements GraphQLResolver<Book> {

    @Autowired
    private AuthorRepository authorRepository;

    public Author getAuthor(Book book) {
        return authorRepository.findByName(book.getName());
    }
}

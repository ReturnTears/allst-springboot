package com.allst.boot.queryresolvers;

import com.allst.boot.model.Author;
import com.allst.boot.repository.AuthorRepository;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Hutu
 * @since 2024-07-30 下午 10:51
 */
@Component
public class AuthorQuery implements GraphQLQueryResolver {
    @Autowired
    private AuthorRepository authorRepository;

    public Author getAuthorById(Integer id) {
        Optional<Author> author = authorRepository.findById(id);
        return author.orElse(null);
    }
}
